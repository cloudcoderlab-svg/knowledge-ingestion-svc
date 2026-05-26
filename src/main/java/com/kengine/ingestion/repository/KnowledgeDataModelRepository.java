package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeDataModelEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge data model entities.
 *
 * <p>Knowledge data models represent database schemas, entities, tables, and data structures
 * extracted from documentation.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on data model entities
 *   <li>Bulk deletion by artifact
 * </ul>
 */
@Repository
public interface KnowledgeDataModelRepository
    extends JpaRepository<KnowledgeDataModelEntity, UUID> {
  /**
   * Deletes all data model entities associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of data model entities deleted
   */
  int deleteByArtifactId(UUID artifactId);
}
