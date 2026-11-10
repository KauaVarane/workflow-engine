
package com.projeto.workflowengine.dto;

import java.util.List;
import java.util.Map;

public class WorkflowDTOs {
    public record SaveRequest(
        String name, 
        String description, 
        String triggerType, 
        Integer intervalMinutes, 
        Boolean active, 
        List<ActionRequest> actions
    ) {}

    public record UpdateRequest(
        String name, 
        Boolean active
    ) {}

    public record ActionRequest(
        String actionType, 
        Integer sequenceOrder, 
        Integer maxRetries, 
        Map<String, Object> parameters
    ) {}
}

