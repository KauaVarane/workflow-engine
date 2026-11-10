package com.projeto.workflowengine.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_step_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "execution_log_id")
    private WorkflowExecutionLog executionLog;

    private Integer stepOrder;
    private String actionType;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String contextSnapshot;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
