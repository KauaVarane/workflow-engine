package com.projeto.workflowengine.dto;

public record StatsResponse(
        long workflows,
        long activeWorkflows,
        long executions,
        long success,
        long failed,
        long running,
        long last24h,
        Double successRate) {
}
