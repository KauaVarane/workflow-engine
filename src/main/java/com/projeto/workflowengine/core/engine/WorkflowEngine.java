package com.projeto.workflowengine.core.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.workflowengine.config.ResourceNotFoundException;
import com.projeto.workflowengine.config.WorkflowProperties;
import com.projeto.workflowengine.core.ExecutionContext;
import com.projeto.workflowengine.core.action.Action;
import com.projeto.workflowengine.core.action.DecisionAction;
import com.projeto.workflowengine.core.entity.Workflow;
import com.projeto.workflowengine.core.entity.WorkflowAction;
import com.projeto.workflowengine.core.entity.WorkflowExecutionLog;
import com.projeto.workflowengine.core.entity.WorkflowStepLog;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import com.projeto.workflowengine.core.entity.enums.TriggerType;
import com.projeto.workflowengine.core.trigger.Trigger;
import com.projeto.workflowengine.repository.WorkflowActionRepository;
import com.projeto.workflowengine.repository.WorkflowExecutionLogRepository;
import com.projeto.workflowengine.repository.WorkflowRepository;
import com.projeto.workflowengine.repository.WorkflowStepLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Executa workflows em segundo plano: avalia o gatilho e roda as ações em ordem,
 * com tentativas (retry), saltos de DECISION e log detalhado de cada passo.
 */
@Slf4j
@Service
public class WorkflowEngine {

    private static final int MAX_MESSAGE = 4000;
    private static final int MAX_SNAPSHOT = 20000;

    private final WorkflowRepository workflowRepository;
    private final WorkflowActionRepository actionRepository;
    private final WorkflowExecutionLogRepository logRepository;
    private final WorkflowStepLogRepository stepLogRepository;
    private final ObjectMapper objectMapper;
    private final WorkflowProperties properties;
    private final Executor executor;

    private final Map<ActionType, Action> actionsMap;
    private final Map<TriggerType, Trigger> triggersMap;

    public WorkflowEngine(
            WorkflowRepository workflowRepository,
            WorkflowActionRepository actionRepository,
            WorkflowExecutionLogRepository logRepository,
            WorkflowStepLogRepository stepLogRepository,
            ObjectMapper objectMapper,
            WorkflowProperties properties,
            @Qualifier("workflowExecutor") Executor executor,
            List<Action> actions,
            List<Trigger> triggers) {
        this.workflowRepository = workflowRepository;
        this.actionRepository = actionRepository;
        this.logRepository = logRepository;
        this.stepLogRepository = stepLogRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.executor = executor;
        this.actionsMap = actions.stream().collect(Collectors.toMap(Action::getType, Function.identity()));
        this.triggersMap = triggers.stream().collect(Collectors.toMap(Trigger::getType, Function.identity()));
    }

    /** Cria o log (RUNNING), agenda a execução na fila e devolve o id do log imediatamente. */
    public Long initiateWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow não encontrado com ID: " + workflowId));

        WorkflowExecutionLog executionLog = logRepository.save(new WorkflowExecutionLog(workflow, LocalDateTime.now()));
        Long logId = executionLog.getId();

        try {
            executor.execute(() -> run(workflowId, logId));
        } catch (RejectedExecutionException e) {
            updateLogStatus(executionLog, "FAILED", "Fila de execuções cheia. Tente novamente em instantes.");
        }
        return logId;
    }

    /** Corpo da execução (roda na thread do executor). */
    public void run(Long workflowId, Long logId) {
        log.info(">>> Executando workflow {} (log {})", workflowId, logId);
        WorkflowExecutionLog executionLog = null;
        try {
            executionLog = logRepository.findById(logId)
                    .orElseThrow(() -> new IllegalStateException("Log de execução não encontrado: " + logId));
            Workflow workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new IllegalStateException("Workflow não encontrado (foi excluído?): " + workflowId));

            Trigger trigger = Optional.ofNullable(triggersMap.get(workflow.getTriggerType()))
                    .orElseThrow(() -> new IllegalStateException("Trigger não implementado para: " + workflow.getTriggerType()));

            ExecutionContext context = new ExecutionContext();
            context.put("workflowId", workflow.getId());
            context.put("workflowName", workflow.getName());
            context.put("executionId", logId);
            context.put("startTime", executionLog.getStartTime().toString());

            if (!trigger.evaluate(context)) {
                updateLogStatus(executionLog, "SKIPPED", "Trigger avaliado como falso");
                return;
            }

            List<WorkflowAction> steps = actionRepository.findByWorkflowIdOrderBySequenceOrderAsc(workflowId);
            if (steps.isEmpty()) {
                updateLogStatus(executionLog, "SUCCESS", "Workflow sem ações para executar");
                return;
            }

            runSteps(executionLog, steps, context);

        } catch (Exception e) {
            String reason = describe(e);
            log.error("Workflow {} falhou: {}", workflowId, reason);
            if (executionLog != null) {
                updateLogStatus(executionLog, "FAILED", reason);
            }
        }
    }

    private void runSteps(WorkflowExecutionLog executionLog, List<WorkflowAction> steps, ExecutionContext context) {
        int index = 0;
        int executed = 0;

        while (index < steps.size()) {
            if (++executed > properties.maxStepsPerExecution()) {
                throw new IllegalStateException("Limite de " + properties.maxStepsPerExecution()
                        + " passos excedido. Verifique se há um loop nas decisões.");
            }

            WorkflowAction step = steps.get(index);
            Action action = Optional.ofNullable(actionsMap.get(step.getActionType()))
                    .orElseThrow(() -> new IllegalStateException("Ação não implementada para: " + step.getActionType()));

            context.put(ExecutionContext.PARAMS_KEY, step.getParameters());
            context.remove(ExecutionContext.NEXT_STEP_KEY);
            LocalDateTime stepStart = LocalDateTime.now();
            int stepNumber = index + 1;
            log.info("Passo {} ({})", stepNumber, step.getActionType());

            String detail;
            try {
                detail = executeWithRetries(action, step, context);
            } catch (RuntimeException e) {
                logStep(executionLog, stepNumber, step.getActionType(), "FAILED", describe(e), context, stepStart);
                throw e;
            }

            Object next = context.get(ExecutionContext.NEXT_STEP_KEY);
            if (step.getActionType() == ActionType.DECISION && next instanceof Integer target) {
                if (target == DecisionAction.END_FLOW) {
                    logStep(executionLog, stepNumber, step.getActionType(), "SUCCESS", "Decisão: encerrar o fluxo", context, stepStart);
                    updateLogStatus(executionLog, "SUCCESS", "Fluxo encerrado por uma decisão no passo " + stepNumber);
                    return;
                }
                if (target < 1 || target > steps.size()) {
                    String msg = "Passo de destino inválido: " + target + " (o fluxo tem " + steps.size() + " passos)";
                    logStep(executionLog, stepNumber, step.getActionType(), "FAILED", msg, context, stepStart);
                    throw new IllegalArgumentException(msg);
                }
                logStep(executionLog, stepNumber, step.getActionType(), "SUCCESS", "Decisão: ir para o passo " + target, context, stepStart);
                index = target - 1;
                continue;
            }

            logStep(executionLog, stepNumber, step.getActionType(), "SUCCESS", detail, context, stepStart);
            index++;
        }

        updateLogStatus(executionLog, "SUCCESS", "Executado com sucesso");
    }

    private String executeWithRetries(Action action, WorkflowAction step, ExecutionContext context) {
        int maxRetries = step.getMaxRetries() == null ? 0 : Math.max(0, step.getMaxRetries());
        for (int attempt = 1; ; attempt++) {
            try {
                action.execute(context);
                return attempt == 1 ? "Executado com sucesso" : "Executado com sucesso na tentativa " + attempt;
            } catch (RuntimeException e) {
                if (attempt > maxRetries) {
                    throw e;
                }
                log.warn("Tentativa {} falhou ({}). Tentando de novo...", attempt, describe(e));
                try {
                    Thread.sleep(300L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Execução interrompida", ie);
                }
            }
        }
    }

    private void logStep(WorkflowExecutionLog executionLog, int order, ActionType type, String status,
                         String message, ExecutionContext context, LocalDateTime start) {
        try {
            WorkflowStepLog stepLog = WorkflowStepLog.builder()
                    .executionLog(executionLog)
                    .stepOrder(order)
                    .actionType(type.name())
                    .status(status)
                    .message(truncate(message, MAX_MESSAGE))
                    .contextSnapshot(snapshot(context))
                    .startTime(start)
                    .endTime(LocalDateTime.now())
                    .build();
            stepLogRepository.save(stepLog);
        } catch (Exception e) {
            log.error("Erro ao gravar log do passo {}: {}", order, e.getMessage());
        }
    }

    /** Foto do contexto depois do passo (sem os campos internos). */
    private String snapshot(ExecutionContext context) {
        Map<String, Object> visible = new LinkedHashMap<>(context.getData());
        visible.remove(ExecutionContext.PARAMS_KEY);
        visible.remove(ExecutionContext.NEXT_STEP_KEY);
        String json;
        try {
            json = objectMapper.writeValueAsString(visible);
        } catch (Exception e) {
            Map<String, String> fallback = new LinkedHashMap<>();
            visible.forEach((k, v) -> fallback.put(k, String.valueOf(v)));
            try {
                json = objectMapper.writeValueAsString(fallback);
            } catch (Exception ex) {
                json = "{}";
            }
        }
        return truncate(json, MAX_SNAPSHOT);
    }

    private void updateLogStatus(WorkflowExecutionLog executionLog, String status, String message) {
        try {
            executionLog.setEndTime(LocalDateTime.now());
            executionLog.setStatus(status);
            executionLog.setMessage(truncate(message, MAX_MESSAGE));
            logRepository.save(executionLog);
        } catch (Exception e) {
            log.error("Não foi possível atualizar o log {}: {}", executionLog.getId(), e.getMessage());
        }
    }

    private static String describe(Throwable e) {
        String message = e.getMessage();
        return (message == null || message.isBlank()) ? e.getClass().getSimpleName() : message;
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "… (truncado)";
    }
}
