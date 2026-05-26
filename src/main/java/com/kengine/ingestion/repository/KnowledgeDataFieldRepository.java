package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeDataFieldEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge data field entities.
 *
 * <p>Knowledge data fields represent individual fields, columns, or attributes within data models.
 * Includes metadata about data types, constraints, and relationships.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on data field entities
 *   <li>Cascade deletion by data model
 * </ul>
 */
@Repository
public interface KnowledgeDataFieldRepository
    extends JpaRepository<KnowledgeDataFieldEntity, UUID> {
  /**
   * Deletes all data fields belonging to a specific data model.
   *
   * <p>Used for cascade deletion when a data model is removed.
   *
   * @param dataModelId Data model UUID
   * @return Number of data fields deleted
   */
  int deleteByDataModelId(UUID dataModelId);
}
