package com.projeto.workflowengine.repository;

  import com.projeto.workflowengine.core.entity.WorkflowAction; // Ajustado para core.entity
  import org.springframework.data.jpa.repository.JpaRepository;
  import java.util.List;

  public interface WorkflowActionRepository extends JpaRepository<WorkflowAction, Long> {
      List<WorkflowAction> findByWorkflowIdOrderBySequenceOrderAsc(Long workflowId);
  }