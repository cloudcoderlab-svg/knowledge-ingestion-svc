package com.kengine.knowledge.repository;

import com.kengine.knowledge.entity.WorkflowStepEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStepEntity, UUID> {
  List<WorkflowStepEntity> findByWorkflowIdOrderBySequenceNumber(UUID workflowId);
}
