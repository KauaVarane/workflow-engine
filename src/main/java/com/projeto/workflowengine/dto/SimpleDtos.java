package com.projeto.workflowengine.dto;

import jakarta.validation.constraints.NotNull;

/** DTOs pequenos de entrada/saída. */
public final class SimpleDtos {

    private SimpleDtos() {
    }

    public record ActiveRequest(@NotNull(message = "O campo 'active' é obrigatório") Boolean active) {
    }

    public record ExecuteResponse(Long logId, String message) {
    }
}
