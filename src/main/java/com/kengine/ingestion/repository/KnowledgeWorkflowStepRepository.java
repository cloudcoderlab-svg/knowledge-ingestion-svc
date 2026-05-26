package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeWorkflowStepEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge workflow step entities.
 *
 * <p>Knowledge workflow steps represent individual steps or actions within a workflow. Each step
 * has a sequence number, description, and optional metadata about actors, systems, or conditions.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on workflow step entities
 *   <li>Cascade deletion by workflow
 * </ul>
 */
@Repository
public interface KnowledgeWorkflowStepRepository
    extends JpaRepository<KnowledgeWorkflowStepEntity, UUID> {
  /**
   * Deletes all workflow steps belonging to a specific workflow.
   *
   * <p>Used for cascade deletion when a workflow is removed.
   *
   * @param workflowId Workflow UUID
   * @return Number of workflow steps deleted
   */
  int deleteByWorkflowId(UUID workflowId);
}
