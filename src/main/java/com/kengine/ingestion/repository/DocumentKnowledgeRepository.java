package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.DocumentKnowledgeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing document knowledge entities.
 *
 * <p>Document knowledge entities represent high-level summaries and metadata about processed
 * documents, including key entities, topics, and document characteristics.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on document knowledge
 *   <li>Query by subject
 *   <li>Bulk deletion by subject or artifact
 * </ul>
 */
@Repository
public interface DocumentKnowledgeRepository extends JpaRepository<DocumentKnowledgeEntity, UUID> {
  /**
   * Deletes all document knowledge entries belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of entries deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Deletes all document knowledge entries associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of entries deleted
   */
  int deleteByArtifactId(UUID artifactId);

  /**
   * Counts total document knowledge entries for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of entries
   */
  long countBySubjectId(UUID subjectId);

  /**
   * Finds all document knowledge entries for a subject.
   *
   * @param subjectId Subject UUID
   * @return List of document knowledge entries
   */
  List<DocumentKnowledgeEntity> findBySubjectId(UUID subjectId);
}
