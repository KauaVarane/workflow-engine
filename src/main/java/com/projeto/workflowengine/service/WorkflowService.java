package com.projeto.workflowengine.service;

import com.projeto.workflowengine.config.ResourceNotFoundException;
import com.projeto.workflowengine.core.entity.Workflow;
import com.projeto.workflowengine.core.entity.WorkflowAction;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import com.projeto.workflowengine.core.entity.enums.TriggerType;
import com.projeto.workflowengine.dto.ActionDto;
import com.projeto.workflowengine.dto.ExecutionLogResponse;
import com.projeto.workflowengine.dto.WorkflowActionRequestDTO;
import com.projeto.workflowengine.dto.WorkflowResponse;
import com.projeto.workflowengine.dto.WorkflowSaveRequest;
import com.projeto.workflowengine.repository.WorkflowActionRepository;
import com.projeto.workflowengine.repository.WorkflowExecutionLogRepository;
import com.projeto.workflowengine.repository.WorkflowRepository;
import com.projeto.workflowengine.util.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowActionRepository actionRepository;
    private final WorkflowExecutionLogRepository logRepository;
    private final com.projeto.workflowengine.repository.user.UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WorkflowResponse> listAll() {
        Map<Long, ExecutionLogResponse> lastByWorkflow = logRepository.findLatestPerWorkflowByOwner(com.projeto.workflowengine.security.CurrentUser.username()).stream()
                .collect(Collectors.toMap(l -> l.getWorkflow().getId(), ExecutionLogResponse::from, (a, b) -> a));
        return workflowRepository.findAllByOwnerUsernameOrderByIdDesc(com.projeto.workflowengine.security.CurrentUser.username()).stream()
                .map(w -> WorkflowResponse.from(w, lastByWorkflow.get(w.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowResponse get(Long id) {
        return toResponse(load(id));
    }

    @Transactional
    public WorkflowResponse create(WorkflowSaveRequest request) {
        Workflow workflow = new Workflow();
        applyFields(workflow, request);
        workflow.setActive(request.active() == null || request.active()); workflow.setOwner(currentOwner());
        if (request.actions() != null) {
            replaceActions(workflow, request.actions());
        }
        return toResponse(workflowRepository.saveAndFlush(workflow));
    }

    @Transactional
    public WorkflowResponse update(Long id, WorkflowSaveRequest request) {
        Workflow workflow = load(id);
        applyFields(workflow, request);
        if (request.active() != null) {
            workflow.setActive(request.active());
        }
        if (request.actions() != null) {
            replaceActions(workflow, request.actions());
        }
        return toResponse(workflowRepository.saveAndFlush(workflow));
    }

    @Transactional
    public WorkflowResponse setActive(Long id, boolean active) {
        Workflow workflow = load(id);
        workflow.setActive(active);
        return toResponse(workflowRepository.saveAndFlush(workflow));
    }

    /** Cópia desativada, para o usuário ajustar antes de ligar. */
    @Transactional
    public WorkflowResponse duplicate(Long id) {
        Workflow original = load(id);
        Workflow copy = new Workflow();
        copy.setName("Cópia de " + original.getName());
        copy.setDescription(original.getDescription());
        copy.setTriggerType(original.getTriggerType());
        copy.setIntervalMinutes(original.getIntervalMinutes());
        copy.setActive(false); copy.setOwner(original.getOwner());
        for (WorkflowAction step : original.getActions()) {
            WorkflowAction clone = new WorkflowAction();
            clone.setActionType(step.getActionType());
            clone.setSequenceOrder(step.getSequenceOrder());
            clone.setMaxRetries(step.getMaxRetries());
            clone.setParameters(step.getParameters());
            clone.setWorkflow(copy);
            copy.getActions().add(clone);
        }
        return toResponse(workflowRepository.saveAndFlush(copy));
    }

    @Transactional
    public void deleteWorkflow(Long id) {
        Workflow workflow = workflowRepository.findByIdAndOwnerUsername(id, com.projeto.workflowengine.security.CurrentUser.username())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow não encontrado com ID: " + id));
        workflowRepository.delete(workflow);
    }

    /** Endpoint legado: adiciona um passo ao final/na ordem informada. */
    @Transactional
    public ActionDto addActionToWorkflow(WorkflowActionRequestDTO dto) {
        Workflow workflow = workflowRepository.findByIdAndOwnerUsername(dto.getWorkflowId(), com.projeto.workflowengine.security.CurrentUser.username())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow não encontrado com ID: " + dto.getWorkflowId()));

        Map<String, Object> params = Json.toMap(dto.getParameters());
        validateStep(1, new ActionDto(null, dto.getActionType(), dto.getSequenceOrder(), params, dto.getMaxRetries()), Integer.MAX_VALUE);

        WorkflowAction action = new WorkflowAction();
        action.setActionType(dto.getActionType());
        action.setSequenceOrder(dto.getSequenceOrder());
        action.setParameters(Json.toJson(params));
        action.setMaxRetries(dto.getMaxRetries() != null ? dto.getMaxRetries() : 0);
        action.setWorkflow(workflow);
        return ActionDto.from(actionRepository.save(action));
    }

    // ---------------------------------------------------------------- helpers

    /** Garante que o workflow existe E e do usuario logado (senao 404). */
    public void assertOwned(Long id) {
        load(id);
    }

    private com.projeto.workflowengine.core.entity.user.User currentOwner() {
        String username = com.projeto.workflowengine.security.CurrentUser.username();
        return username == null ? null : userRepository.findByUsername(username).orElse(null);
    }

    private Workflow load(Long id) {
        return workflowRepository.findWithActionsByIdAndOwnerUsername(id, com.projeto.workflowengine.security.CurrentUser.username())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow não encontrado com ID: " + id));
    }

    private WorkflowResponse toResponse(Workflow workflow) {
        ExecutionLogResponse last = workflow.getId() == null ? null
                : logRepository.search(workflow.getId(), null, PageRequest.of(0, 1)).stream()
                        .findFirst().map(ExecutionLogResponse::from).orElse(null);
        return WorkflowResponse.from(workflow, last);
    }

    private void applyFields(Workflow workflow, WorkflowSaveRequest request) {
        workflow.setName(request.name().trim());
        String description = request.description();
        workflow.setDescription(description == null || description.isBlank() ? null : description.trim());
        workflow.setTriggerType(request.triggerType());
        workflow.setIntervalMinutes(request.triggerType() == TriggerType.SCHEDULE
                ? (request.intervalMinutes() == null ? 1 : request.intervalMinutes())
                : null);
    }

    /** A posição na lista define a ordem. Substitui todos os passos atuais. */
    private void replaceActions(Workflow workflow, List<ActionDto> steps) {
        for (int i = 0; i < steps.size(); i++) {
            validateStep(i + 1, steps.get(i), steps.size());
        }
        workflow.getActions().clear();
        int order = 1;
        for (ActionDto dto : steps) {
            WorkflowAction action = new WorkflowAction();
            action.setActionType(dto.actionType());
            action.setSequenceOrder(order++);
            action.setMaxRetries(dto.maxRetries() == null ? 0 : dto.maxRetries());
            action.setParameters(Json.toJson(dto.parameters()));
            action.setWorkflow(workflow);
            workflow.getActions().add(action);
        }
    }

    private void validateStep(int number, ActionDto step, int totalSteps) {
        String prefix = "Passo " + number + " (" + step.actionType() + "): ";
        Map<String, Object> params = step.parameters() == null ? Map.of() : step.parameters();

        switch (step.actionType()) {
            case HTTP -> {
                String url = text(params, "url");
                if (url == null || !(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("{{"))) {
                    throw new IllegalArgumentException(prefix + "informe uma URL começando com http:// ou https://");
                }
            }
            case DATABASE -> {
                if (text(params, "query") == null) {
                    throw new IllegalArgumentException(prefix + "informe a query SQL");
                }
            }
            case DECISION -> {
                if (text(params, "variable") == null) {
                    throw new IllegalArgumentException(prefix + "informe a variável a ser comparada");
                }
                checkTarget(prefix, params.get("success_step"), totalSteps);
                checkTarget(prefix, params.get("failure_step"), totalSteps);
            }
            default -> {
            }
        }
    }

    private void checkTarget(String prefix, Object value, int totalSteps) {
        if (value == null || String.valueOf(value).isBlank()) {
            return;
        }
        String raw = String.valueOf(value).trim();
        if (raw.equalsIgnoreCase("end") || raw.equalsIgnoreCase("fim")) {
            return;
        }
        int target;
        try {
            target = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(prefix + "passo de destino inválido: " + raw);
        }
        if (target != -1 && (target < 1 || target > totalSteps)) {
            throw new IllegalArgumentException(prefix + "o passo de destino " + target
                    + " não existe (use de 1 a " + totalSteps + ", ou -1 para encerrar)");
        }
    }

    private String text(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
