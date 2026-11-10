package com.projeto.workflowengine.core.engine;

import com.projeto.workflowengine.core.entity.Workflow;
import com.projeto.workflowengine.core.entity.WorkflowExecutionLog;
import com.projeto.workflowengine.core.entity.enums.TriggerType;
import com.projeto.workflowengine.repository.WorkflowExecutionLogRepository;
import com.projeto.workflowengine.repository.WorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dispara os workflows SCHEDULE ativos respeitando o intervalo de cada um (padrão: 1 minuto)
 * e corrige execuções que ficaram "RUNNING" porque o servidor foi reiniciado.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "workflow.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowScheduler {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionLogRepository logRepository;
    private final WorkflowEngine engine;

    public WorkflowScheduler(WorkflowRepository workflowRepository,
                             WorkflowExecutionLogRepository logRepository,
                             WorkflowEngine engine) {
        this.workflowRepository = workflowRepository;
        this.logRepository = logRepository;
        this.engine = engine;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedExecutions() {
        List<WorkflowExecutionLog> stuck = logRepository.findByStatus("RUNNING");
        for (WorkflowExecutionLog executionLog : stuck) {
            executionLog.setStatus("FAILED");
            executionLog.setEndTime(LocalDateTime.now());
            executionLog.setMessage("Execução interrompida: o servidor foi reiniciado antes de terminar.");
            logRepository.save(executionLog);
        }
        if (!stuck.isEmpty()) {
            log.warn("{} execução(ões) que estavam RUNNING foram marcadas como FAILED.", stuck.size());
        }
    }

    @Scheduled(fixedDelay = 15_000, initialDelay = 20_000)
    public void processScheduledWorkflows() {
        LocalDateTime now = LocalDateTime.now();
        for (Workflow workflow : workflowRepository.findByTriggerTypeAndActiveTrue(TriggerType.SCHEDULE)) {
            int interval = Math.max(1, workflow.getIntervalMinutes() == null ? 1 : workflow.getIntervalMinutes());
            LocalDateTime last = workflow.getLastTriggeredAt();
            if (last == null || !last.plusMinutes(interval).isAfter(now)) {
                log.info("Disparando workflow agendado: {}", workflow.getName());
                workflowRepository.touchLastTriggered(workflow.getId(), now);
                try {
                    engine.initiateWorkflow(workflow.getId());
                } catch (Exception e) {
                    log.error("Falha ao disparar o workflow {}: {}", workflow.getId(), e.getMessage());
                }
            }
        }
    }
}
