package com.projeto.workflowengine.dto;

import com.projeto.workflowengine.core.entity.Workflow;
import com.projeto.workflowengine.core.entity.enums.TriggerType;

import java.time.LocalDateTime;
import java.util.List;

public record WorkflowResponse(
        Long id,
        String name,
        String description,
        TriggerType triggerType,
        boolean active,
        Integer intervalMinutes,
        LocalDateTime lastTriggeredAt,
        List<ActionDto> actions,
        ExecutionLogResponse lastExecution) {

    public static WorkflowResponse from(Workflow workflow, ExecutionLogResponse lastExecution) {
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getTriggerType(),
                workflow.isActive(),
                workflow.getIntervalMinutes(),
                workflow.getLastTriggeredAt(),
                workflow.getActions().stream().map(ActionDto::from).toList(),
                lastExecution);
    }
}
