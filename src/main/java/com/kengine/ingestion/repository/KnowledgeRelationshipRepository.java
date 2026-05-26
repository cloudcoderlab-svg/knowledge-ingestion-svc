package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeRelationshipEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge relationship entities.
 *
 * <p>Knowledge relationships represent connections between knowledge entities such as components,
 * workflows, business rules, and data models. Examples: "Component A depends on Component B",
 * "Workflow X invokes API Y", "Rule A validates Field B".
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on relationships
 *   <li>Bulk deletion by subject
 *   <li>Support for directed graph edges between entities
 * </ul>
 */
@Repository
public interface KnowledgeRelationshipRepository
    extends JpaRepository<KnowledgeRelationshipEntity, String> {
  /**
   * Deletes all relationships belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of relationships deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Counts total relationships for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of relationships
   */
  long countBySubjectId(UUID subjectId);
}
