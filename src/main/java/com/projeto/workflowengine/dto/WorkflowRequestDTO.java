package com.projeto.workflowengine.dto;

import com.projeto.workflowengine.core.entity.enums.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkflowRequestDTO {
    @NotBlank(message = "O nome do workflow é obrigatório")
    private String name;

    @NotNull(message = "O tipo do trigger é obrigatório")
    private TriggerType triggerType;
}