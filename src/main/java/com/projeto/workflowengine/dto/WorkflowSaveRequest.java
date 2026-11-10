package com.projeto.workflowengine.dto;

import com.projeto.workflowengine.core.entity.enums.TriggerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Corpo de criação (POST) e edição (PUT) de um workflow.
 * No PUT, se "actions" vier preenchida ela SUBSTITUI todos os passos; se vier null, os passos ficam como estão.
 */
public record WorkflowSaveRequest(
        @NotBlank(message = "O nome do workflow é obrigatório")
        @Size(max = 255, message = "O nome pode ter no máximo 255 caracteres") String name,
        @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres") String description,
        @NotNull(message = "O tipo do trigger é obrigatório") TriggerType triggerType,
        @Min(value = 1, message = "O intervalo mínimo é 1 minuto")
        @Max(value = 10080, message = "O intervalo máximo é 10080 minutos (7 dias)") Integer intervalMinutes,
        Boolean active,
        @Valid List<ActionDto> actions) {
}
