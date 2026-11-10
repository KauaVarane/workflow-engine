package com.projeto.workflowengine.controller;

import com.projeto.workflowengine.core.engine.WorkflowEngine;
import com.projeto.workflowengine.dto.ActionDto;
import com.projeto.workflowengine.dto.SimpleDtos.ActiveRequest;
import com.projeto.workflowengine.dto.SimpleDtos.ExecuteResponse;
import com.projeto.workflowengine.dto.WorkflowActionRequestDTO;
import com.projeto.workflowengine.dto.WorkflowResponse;
import com.projeto.workflowengine.dto.WorkflowSaveRequest;
import com.projeto.workflowengine.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workflows", description = "CRUD de workflows e seus passos, mais execução manual")
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowEngine workflowEngine;
    private final WorkflowService workflowService;

    @Operation(summary = "Lista todos os workflows (com passos e última execução)")
    @GetMapping
    public List<WorkflowResponse> listAll() {
        return workflowService.listAll();
    }

    @Operation(summary = "Busca um workflow pelo id")
    @GetMapping("/{id}")
    public WorkflowResponse get(@PathVariable Long id) {
        return workflowService.get(id);
    }

    @Operation(summary = "Cria um workflow (os passos em 'actions' são opcionais)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse create(@Valid @RequestBody WorkflowSaveRequest request) {
        return workflowService.create(request);
    }

    @Operation(summary = "Edita um workflow; se 'actions' vier preenchida, substitui todos os passos")
    @PutMapping("/{id}")
    public WorkflowResponse update(@PathVariable Long id, @Valid @RequestBody WorkflowSaveRequest request) {
        return workflowService.update(id, request);
    }

    @Operation(summary = "Ativa/desativa o disparo automático (agendamento)")
    @PatchMapping("/{id}/active")
    public WorkflowResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return workflowService.setActive(id, request.active());
    }

    @Operation(summary = "Duplica o workflow (a cópia nasce desativada)")
    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse duplicate(@PathVariable Long id) {
        return workflowService.duplicate(id);
    }

    @Operation(summary = "Exclui o workflow, seus passos e seu histórico")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
    }

    @Operation(summary = "Executa agora (em segundo plano). Devolve o id do log para acompanhar")
    @PostMapping("/{id}/execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecuteResponse execute(@PathVariable Long id) {
        workflowService.assertOwned(id);
        Long logId = workflowEngine.initiateWorkflow(id);
        return new ExecuteResponse(logId, "Execução iniciada! Acompanhe pelo Log ID: " + logId);
    }

    @Operation(summary = "[Legado] Adiciona um passo a um workflow existente")
    @PostMapping("/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public ActionDto addAction(@Valid @RequestBody WorkflowActionRequestDTO dto) {
        return workflowService.addActionToWorkflow(dto);
    }
}
