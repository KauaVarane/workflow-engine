package com.projeto.workflowengine.controller;

import com.projeto.workflowengine.config.ResourceNotFoundException;
import com.projeto.workflowengine.dto.StepLogResponse;
import com.projeto.workflowengine.repository.WorkflowExecutionLogRepository;
import com.projeto.workflowengine.repository.WorkflowStepLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Execuções")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class WorkflowLogController {

    private final WorkflowStepLogRepository stepLogRepository;
    private final WorkflowExecutionLogRepository logRepository;

    @Operation(summary = "Linha do tempo: o que aconteceu em cada passo da execução")
    @GetMapping("/{id}/details")
    public List<StepLogResponse> getStepDetails(@PathVariable Long id) {
        if (!logRepository.existsByIdAndWorkflowOwnerUsername(id, com.projeto.workflowengine.security.CurrentUser.username())) {
            throw new ResourceNotFoundException("Execução não encontrada com ID: " + id);
        }
        return stepLogRepository.findByExecutionLogIdOrderByStepOrderAsc(id).stream()
                .map(StepLogResponse::from)
                .toList();
    }
}
