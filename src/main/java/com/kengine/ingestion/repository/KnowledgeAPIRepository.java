package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeAPIEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge API entities.
 *
 * <p>Knowledge API entities represent REST APIs, endpoints, and integration points extracted from
 * documentation. Includes metadata about HTTP methods, paths, request/response formats, and
 * authentication.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on API entities
 *   <li>Bulk deletion by subject or artifact
 * </ul>
 */
@Repository
public interface KnowledgeAPIRepository extends JpaRepository<KnowledgeAPIEntity, UUID> {
  /**
   * Deletes all API entities belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of API entities deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Deletes all API entities associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of API entities deleted
   */
  int deleteByArtifactId(UUID artifactId);

  /**
   * Counts total API entities for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of API entities
   */
  long countBySubjectId(UUID subjectId);
}
