package com.projeto.workflowengine.repository;

import com.projeto.workflowengine.core.entity.Workflow;
import com.projeto.workflowengine.core.entity.enums.TriggerType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    @EntityGraph(attributePaths = "actions")
    List<Workflow> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "actions")
    Optional<Workflow> findWithActionsById(Long id);

    List<Workflow> findByTriggerTypeAndActiveTrue(TriggerType triggerType);

    long countByActiveTrue();

    @EntityGraph(attributePaths = "actions")
    List<Workflow> findAllByOwnerUsernameOrderByIdDesc(String username);

    @EntityGraph(attributePaths = "actions")
    Optional<Workflow> findWithActionsByIdAndOwnerUsername(Long id, String username);

    Optional<Workflow> findByIdAndOwnerUsername(Long id, String username);

    long countByOwnerUsername(String username);

    long countByOwnerUsernameAndActiveTrue(String username);

    @Modifying
    @Transactional
    @Query("update Workflow w set w.lastTriggeredAt = :ts where w.id = :id")
    void touchLastTriggered(@Param("id") Long id, @Param("ts") LocalDateTime ts);
}
