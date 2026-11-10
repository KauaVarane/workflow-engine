package com.projeto.workflowengine.controller;

import com.projeto.workflowengine.dto.StatsResponse;
import com.projeto.workflowengine.repository.WorkflowExecutionLogRepository;
import com.projeto.workflowengine.repository.WorkflowRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionLogRepository logRepository;

    @Operation(summary = "Números do dashboard")
    @GetMapping
    public StatsResponse stats() {
        long success = logRepository.countByStatusAndWorkflowOwnerUsername("SUCCESS", com.projeto.workflowengine.security.CurrentUser.username());
        long failed = logRepository.countByStatusAndWorkflowOwnerUsername("FAILED", com.projeto.workflowengine.security.CurrentUser.username());
        long finished = success + failed;
        Double rate = finished == 0 ? null : Math.round(success * 1000.0 / finished) / 10.0;

        return new StatsResponse(
                workflowRepository.countByOwnerUsername(com.projeto.workflowengine.security.CurrentUser.username()),
                workflowRepository.countByOwnerUsernameAndActiveTrue(com.projeto.workflowengine.security.CurrentUser.username()),
                logRepository.countByWorkflowOwnerUsername(com.projeto.workflowengine.security.CurrentUser.username()),
                success,
                failed,
                logRepository.countByStatusAndWorkflowOwnerUsername("RUNNING", com.projeto.workflowengine.security.CurrentUser.username()),
                logRepository.countByStartTimeAfterAndWorkflowOwnerUsername(LocalDateTime.now().minusHours(24), com.projeto.workflowengine.security.CurrentUser.username()),
                rate);
    }
}
