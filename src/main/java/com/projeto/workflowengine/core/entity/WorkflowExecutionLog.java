package com.projeto.workflowengine.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class WorkflowExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** RUNNING, SUCCESS, FAILED ou SKIPPED */
    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;

    @OneToMany(mappedBy = "executionLog", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private List<WorkflowStepLog> steps = new ArrayList<>();

    public WorkflowExecutionLog(Workflow workflow, LocalDateTime startTime) {
        this.workflow = workflow;
        this.startTime = startTime;
        this.status = "RUNNING";
    }
}
