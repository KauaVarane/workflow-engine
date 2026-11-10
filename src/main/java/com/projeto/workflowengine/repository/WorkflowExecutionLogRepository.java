package com.projeto.workflowengine.repository;

import com.projeto.workflowengine.core.entity.WorkflowExecutionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkflowExecutionLogRepository extends JpaRepository<WorkflowExecutionLog, Long> {

    @Query("select l from WorkflowExecutionLog l join fetch l.workflow w "
            + "where (:workflowId is null or w.id = :workflowId) "
            + "and (:status is null or l.status = :status) "
            + "order by l.id desc")
    List<WorkflowExecutionLog> search(@Param("workflowId") Long workflowId,
                                      @Param("status") String status,
                                      Pageable pageable);

    /** Última execução de cada workflow (para mostrar o status na lista). */
    @Query("select l from WorkflowExecutionLog l join fetch l.workflow "
            + "where l.id in (select max(l2.id) from WorkflowExecutionLog l2 group by l2.workflow.id)")
    List<WorkflowExecutionLog> findLatestPerWorkflow();

    List<WorkflowExecutionLog> findByStatus(String status);

    long countByStatus(String status);

    @Query("select l from WorkflowExecutionLog l join fetch l.workflow w "
            + "where w.owner.username = :username "
            + "and (:workflowId is null or w.id = :workflowId) "
            + "and (:status is null or l.status = :status) "
            + "order by l.id desc")
    List<WorkflowExecutionLog> searchByOwner(@Param("username") String username,
                                             @Param("workflowId") Long workflowId,
                                             @Param("status") String status,
                                             Pageable pageable);

    @Query("select l from WorkflowExecutionLog l join fetch l.workflow w "
            + "where w.owner.username = :username "
            + "and l.id in (select max(l2.id) from WorkflowExecutionLog l2 group by l2.workflow.id)")
    List<WorkflowExecutionLog> findLatestPerWorkflowByOwner(@Param("username") String username);

    java.util.Optional<WorkflowExecutionLog> findByIdAndWorkflowOwnerUsername(Long id, String username);

    boolean existsByIdAndWorkflowOwnerUsername(Long id, String username);

    long countByWorkflowOwnerUsername(String username);

    long countByStatusAndWorkflowOwnerUsername(String status, String username);

    long countByStartTimeAfterAndWorkflowOwnerUsername(LocalDateTime since, String username);

    long countByStartTimeAfter(LocalDateTime since);
}
