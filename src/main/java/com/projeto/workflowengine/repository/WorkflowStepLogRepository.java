package com.projeto.workflowengine.repository;

import com.projeto.workflowengine.core.entity.WorkflowStepLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowStepLogRepository extends JpaRepository<WorkflowStepLog, Long> {
    List<WorkflowStepLog> findByExecutionLogIdOrderByStepOrderAsc(Long executionLogId);
}
