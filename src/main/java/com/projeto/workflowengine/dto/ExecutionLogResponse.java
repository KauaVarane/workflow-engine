package com.projeto.workflowengine.dto;

import com.projeto.workflowengine.core.entity.WorkflowExecutionLog;

import java.time.Duration;
import java.time.LocalDateTime;

public record ExecutionLogResponse(
        Long id,
        Long workflowId,
        String workflowName,
        String status,
        String message,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationMs) {

    public static ExecutionLogResponse from(WorkflowExecutionLog log) {
        Long duration = (log.getStartTime() != null && log.getEndTime() != null)
                ? Duration.between(log.getStartTime(), log.getEndTime()).toMillis()
                : null;
        return new ExecutionLogResponse(
                log.getId(),
                log.getWorkflow() != null ? log.getWorkflow().getId() : null,
                log.getWorkflow() != null ? log.getWorkflow().getName() : null,
                log.getStatus(),
                log.getMessage(),
                log.getStartTime(),
                log.getEndTime(),
                duration);
    }
}
