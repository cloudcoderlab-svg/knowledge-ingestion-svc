package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeResourceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge resource entities.
 *
 * <p>Knowledge resources represent external resources, dependencies, libraries, frameworks, and
 * tools mentioned in documentation.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on resource entities
 *   <li>Bulk deletion by subject or artifact
 * </ul>
 */
@Repository
public interface KnowledgeResourceRepository extends JpaRepository<KnowledgeResourceEntity, UUID> {
  /**
   * Deletes all resource entities belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of resource entities deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Deletes all resource entities associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of resource entities deleted
   */
  int deleteByArtifactId(UUID artifactId);

  /**
   * Counts total resource entities for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of resource entities
   */
  long countBySubjectId(UUID subjectId);
}
