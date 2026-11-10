package com.projeto.workflowengine.dto;

import com.projeto.workflowengine.core.entity.WorkflowStepLog;

import java.time.Duration;
import java.time.LocalDateTime;

public record StepLogResponse(
        Long id,
        Integer stepOrder,
        String actionType,
        String status,
        String message,
        String contextSnapshot,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationMs) {

    public static StepLogResponse from(WorkflowStepLog step) {
        Long duration = (step.getStartTime() != null && step.getEndTime() != null)
                ? Duration.between(step.getStartTime(), step.getEndTime()).toMillis()
                : null;
        return new StepLogResponse(
                step.getId(),
                step.getStepOrder(),
                step.getActionType(),
                step.getStatus(),
                step.getMessage(),
                step.getContextSnapshot(),
                step.getStartTime(),
                step.getEndTime(),
                duration);
    }
}
