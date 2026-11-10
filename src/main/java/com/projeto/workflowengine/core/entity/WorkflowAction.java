package com.projeto.workflowengine.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.projeto.workflowengine.core.entity.enums.ActionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class WorkflowAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private Integer sequenceOrder;

    private Integer maxRetries = 0;

    @Column(columnDefinition = "TEXT")
    private String parameters; // Armazena JSON com as configs da ação

    @ManyToOne
    @JoinColumn(name = "workflow_id")
    @JsonIgnore
    private Workflow workflow;
}