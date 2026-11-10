package com.projeto.workflowengine.config;

import com.projeto.workflowengine.core.entity.enums.ActionType;
import com.projeto.workflowengine.core.entity.enums.TriggerType;
import com.projeto.workflowengine.dto.ActionDto;
import com.projeto.workflowengine.dto.WorkflowSaveRequest;
import com.projeto.workflowengine.repository.WorkflowRepository;
import com.projeto.workflowengine.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Cria um workflow de exemplo quando o banco está vazio.
 * Só roda com workflow.seed-demo-data=true (substitui o antigo TesteBanco, que APAGAVA os dados a cada start).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "workflow.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final WorkflowRepository workflowRepository;
    private final WorkflowService workflowService;

    public DemoDataSeeder(WorkflowRepository workflowRepository, WorkflowService workflowService) {
        this.workflowRepository = workflowRepository;
        this.workflowService = workflowService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (workflowRepository.count() > 0) {
            return;
        }
        workflowService.create(new WorkflowSaveRequest(
                "Exemplo: consultar API e notificar",
                "Chama uma API pública, confere o status e notifica. Se falhar, encerra o fluxo.",
                TriggerType.HTTP, null, true,
                List.of(
                        new ActionDto(null, ActionType.LOG, null,
                                Map.of("message", "Iniciando {{workflowName}} (execução {{executionId}})"), 0),
                        new ActionDto(null, ActionType.HTTP, null,
                                Map.of("url", "https://api.github.com/zen", "method", "GET", "result_key", "zen"), 2),
                        new ActionDto(null, ActionType.DECISION, null,
                                Map.of("variable", "zen_status", "operator", "EQUALS", "expected_value", "200",
                                        "failure_step", -1), 0),
                        new ActionDto(null, ActionType.NOTIFICATION, null,
                                Map.of("user", "admin", "message", "A API respondeu: {{zen}}"), 0))));
        log.info("Workflow de exemplo criado.");
    }
}
