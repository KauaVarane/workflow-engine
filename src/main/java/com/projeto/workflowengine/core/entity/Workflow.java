package com.projeto.workflowengine.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.projeto.workflowengine.core.entity.enums.TriggerType;
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
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;

    /** Desativado = o agendador não dispara. A execução manual continua permitida. */
    @Column(nullable = false)
    private boolean active = true;

    /** Só para SCHEDULE: de quantos em quantos minutos executar (padrão 1). */
    private Integer intervalMinutes;

    private LocalDateTime lastTriggeredAt;

    /** Dono do workflow: cada usuario so enxerga os seus. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private com.projeto.workflowengine.core.entity.user.User owner;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @JsonIgnore
    private List<WorkflowAction> actions = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<WorkflowExecutionLog> logs = new ArrayList<>();
}
