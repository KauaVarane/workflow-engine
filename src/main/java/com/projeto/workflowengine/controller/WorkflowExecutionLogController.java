package com.projeto.workflowengine.controller;

import com.projeto.workflowengine.config.ResourceNotFoundException;
import com.projeto.workflowengine.core.entity.WorkflowExecutionLog;
import com.projeto.workflowengine.dto.ExecutionLogResponse;
import com.projeto.workflowengine.repository.WorkflowExecutionLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Execuções", description = "Histórico de execuções dos workflows")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class WorkflowExecutionLogController {

    private final WorkflowExecutionLogRepository logRepository;

    @Operation(summary = "Lista execuções (mais recentes primeiro). Filtros opcionais: workflowId, status, limit")
    @GetMapping
    @Transactional(readOnly = true)
    public List<ExecutionLogResponse> list(@RequestParam(required = false) Long workflowId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "100") int limit) {
        int size = Math.min(Math.max(limit, 1), 500);
        String normalized = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        return logRepository.searchByOwner(com.projeto.workflowengine.security.CurrentUser.username(), workflowId, normalized, PageRequest.of(0, size)).stream()
                .map(ExecutionLogResponse::from)
                .toList();
    }

    @Operation(summary = "Busca uma execução pelo id")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ExecutionLogResponse getById(@PathVariable Long id) {
        return logRepository.findByIdAndWorkflowOwnerUsername(id, com.projeto.workflowengine.security.CurrentUser.username())
                .map(ExecutionLogResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Execução não encontrada com ID: " + id));
    }

    @Operation(summary = "Exclui uma execução e seus passos (execuções em andamento não podem ser excluídas)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        WorkflowExecutionLog executionLog = logRepository.findByIdAndWorkflowOwnerUsername(id, com.projeto.workflowengine.security.CurrentUser.username())
                .orElseThrow(() -> new ResourceNotFoundException("Execução não encontrada com ID: " + id));
        if ("RUNNING".equals(executionLog.getStatus())) {
            throw new IllegalArgumentException("Esta execução ainda está em andamento.");
        }
        logRepository.delete(executionLog);
    }

    @Operation(summary = "Limpa o histórico (todas as execuções finalizadas, ou só as de um workflow)")
    @DeleteMapping
    @Transactional
    public void clear(@RequestParam(required = false) Long workflowId) {
        List<WorkflowExecutionLog> logs = logRepository.searchByOwner(com.projeto.workflowengine.security.CurrentUser.username(), workflowId, null, Pageable.unpaged()).stream()
                .filter(l -> !"RUNNING".equals(l.getStatus()))
                .toList();
        logRepository.deleteAll(logs);
    }
}
