package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeWorkflowStepEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeWorkflowStepRepository
    extends JpaRepository<KnowledgeWorkflowStepEntity, UUID> {
  int deleteByWorkflowId(UUID workflowId);
}
