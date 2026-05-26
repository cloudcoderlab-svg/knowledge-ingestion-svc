package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeComponentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge component entities.
 *
 * <p>Provides data access for architectural components extracted from documentation. Supports
 * vector similarity search to find components based on semantic meaning.
 *
 * <p>Components represent system building blocks like services, libraries, databases, APIs, and
 * other architectural elements.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on components
 *   <li>Vector similarity search using pgvector
 *   <li>Filtering by subject and domain
 *   <li>Bulk deletion operations
 * </ul>
 */
@Repository
public interface KnowledgeComponentRepository
    extends JpaRepository<KnowledgeComponentEntity, UUID> {
  /**
   * Finds all components for a subject, ordered by creation time (newest first).
   *
   * @param subjectId Subject UUID
   * @return List of components ordered by creation time descending
   */
  List<KnowledgeComponentEntity> findBySubjectIdOrderByCreatedAtDesc(UUID subjectId);

  /**
   * Deletes all components belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of components deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Deletes all components associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of components deleted
   */
  int deleteByArtifactId(UUID artifactId);

  /**
   * Counts total components for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of components
   */
  long countBySubjectId(UUID subjectId);

  /**
   * Finds semantically similar components using vector similarity search.
   *
   * <p>Uses PostgreSQL pgvector's cosine distance operator to find components with embeddings most
   * similar to the query embedding. Results are ordered by similarity (highest first).
   *
   * <p>Optional domain filter restricts results to a specific business domain.
   *
   * @param subjectId Subject UUID (required)
   * @param embedding Query embedding in pgvector string format: "[0.1,0.2,0.3,...]"
   * @param domainId Optional domain UUID filter (null = all domains)
   * @param limit Maximum number of results to return
   * @return List of Object arrays containing: [component_id, artifact_id, subject_id, domain_id,
   *     component_name, component_type, category, description, responsibility, technology,
   *     capability, owner, lifecycle, confidence, similarity_score]
   */
  @Query(
      value =
          "SELECT component_id, artifact_id, subject_id, domain_id, component_name, "
              + "component_type, category, description, responsibility, technology, "
              + "capability, owner, lifecycle, confidence, "
              + "(1 - (embedding <=> CAST(:embedding AS vector))) as similarity "
              + "FROM knowledge.knowledge_components "
              + "WHERE subject_id = :subjectId "
              + "AND (:domainId IS NULL OR domain_id = :domainId) "
              + "AND embedding IS NOT NULL "
              + "ORDER BY embedding <=> CAST(:embedding AS vector) "
              + "LIMIT :limit",
      nativeQuery = true)
  List<Object[]> findSimilarByEmbedding(
      @Param("subjectId") UUID subjectId,
      @Param("embedding") String embedding,
      @Param("domainId") UUID domainId,
      @Param("limit") int limit);
}
