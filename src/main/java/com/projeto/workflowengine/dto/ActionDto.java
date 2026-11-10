package com.projeto.workflowengine.dto;

import com.projeto.workflowengine.core.entity.WorkflowAction;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import com.projeto.workflowengine.util.Json;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Um passo do workflow. A posição na lista define a ordem (passo 1, 2, 3...).
 * "parameters" é um objeto JSON; as chaves dependem do tipo (veja as ações em core/action).
 */
public record ActionDto(
        Long id,
        @NotNull(message = "O tipo da ação é obrigatório") ActionType actionType,
        Integer sequenceOrder,
        Map<String, Object> parameters,
        @Min(value = 0, message = "maxRetries não pode ser negativo")
        @Max(value = 10, message = "maxRetries máximo é 10") Integer maxRetries) {

    public static ActionDto from(WorkflowAction action) {
        return new ActionDto(
                action.getId(),
                action.getActionType(),
                action.getSequenceOrder(),
                Json.toMapSafe(action.getParameters()),
                action.getMaxRetries() == null ? 0 : action.getMaxRetries());
    }
}
