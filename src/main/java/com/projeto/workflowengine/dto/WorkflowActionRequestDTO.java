package com.projeto.workflowengine.dto;

import com.projeto.workflowengine.core.entity.enums.ActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class WorkflowActionRequestDTO {
    @NotNull(message = "O tipo da ação é obrigatório")
    private ActionType actionType;

    @NotNull
    @Positive(message = "A ordem deve ser um número positivo")
    private Integer sequenceOrder;

    @NotNull(message = "O ID do workflow é obrigatório")
    private Long workflowId;

    private String parameters; // Ex: {"message": "Ola Mundo", "email": "teste@email.com"}
    private Integer maxRetries = 0;
}