package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeIntegrationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge integration entities.
 *
 * <p>Knowledge integration entities represent system integrations, external service connections,
 * message queues, webhooks, and other integration points described in documentation.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on integration entities
 *   <li>Bulk deletion by artifact
 * </ul>
 */
@Repository
public interface KnowledgeIntegrationRepository
    extends JpaRepository<KnowledgeIntegrationEntity, UUID> {
  /**
   * Deletes all integration entities associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of integration entities deleted
   */
  int deleteByArtifactId(UUID artifactId);
}
