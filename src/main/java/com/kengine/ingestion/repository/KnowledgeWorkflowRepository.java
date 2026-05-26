package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeWorkflowEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge workflow entities.
 *
 * <p>Provides data access for workflows extracted from documentation. Supports vector similarity
 * search to find workflows based on semantic meaning.
 *
 * <p>Workflows represent business processes, procedures, sequences of actions, and operational
 * flows described in documentation.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on workflows
 *   <li>Vector similarity search using pgvector
 *   <li>Filtering by subject and domain
 *   <li>Bulk deletion operations
 * </ul>
 */
@Repository
public interface KnowledgeWorkflowRepository extends JpaRepository<KnowledgeWorkflowEntity, UUID> {
  /**
   * Deletes all workflows belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of workflows deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Deletes all workflows associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of workflows deleted
   */
  int deleteByArtifactId(UUID artifactId);

  /**
   * Counts total workflows for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of workflows
   */
  long countBySubjectId(UUID subjectId);

  /**
   * Finds semantically similar workflows using vector similarity search.
   *
   * <p>Uses PostgreSQL pgvector's cosine distance operator to find workflows with embeddings most
   * similar to the query embedding. Results are ordered by similarity (highest first).
   *
   * <p>Useful for finding workflows related to specific business processes, triggers, or outcomes.
   *
   * @param subjectId Subject UUID (required)
   * @param embedding Query embedding in pgvector string format: "[0.1,0.2,0.3,...]"
   * @param domainId Optional domain UUID filter (null = all domains)
   * @param limit Maximum number of results to return
   * @return List of Object arrays containing: [workflow_id, artifact_id, subject_id, domain_id,
   *     workflow_name, trigger_text, outcome_text, owner, confidence, similarity_score]
   */
  @Query(
      value =
          "SELECT workflow_id, artifact_id, subject_id, domain_id, workflow_name, "
              + "trigger_text, outcome_text, owner, confidence, "
              + "(1 - (embedding <=> CAST(:embedding AS vector))) as similarity "
              + "FROM knowledge.knowledge_workflows "
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
