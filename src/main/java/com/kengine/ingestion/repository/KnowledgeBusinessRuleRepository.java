package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeBusinessRuleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge business rule entities.
 *
 * <p>Provides data access for business rules extracted from documentation. Supports both
 * traditional querying by rule type and vector similarity search for semantic matching.
 *
 * <p>Business rules represent conditional logic, constraints, policies, validations, and other
 * decision-making criteria found in documentation.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on business rules
 *   <li>Vector similarity search using pgvector
 *   <li>Filtering by rule type, domain, and confidence
 *   <li>Paginated queries with/without embeddings
 *   <li>Bulk deletion operations
 * </ul>
 */
@Repository
public interface KnowledgeBusinessRuleRepository
    extends JpaRepository<KnowledgeBusinessRuleEntity, UUID> {

  /**
   * Finds paginated business rules of a specific type, ordered by confidence.
   *
   * @param ruleType Type of rule (e.g., "VALIDATION", "CONSTRAINT", "POLICY")
   * @param pageable Pagination parameters
   * @return Page of business rules ordered by confidence descending
   */
  Page<KnowledgeBusinessRuleEntity> findByRuleTypeOrderByConfidenceDesc(
      String ruleType, Pageable pageable);

  /**
   * Finds all business rules without embedding data, ordered by confidence.
   *
   * <p>Excludes the embedding vector field for performance when full entity data is not needed.
   * Useful for list views and summaries.
   *
   * @param pageable Pagination parameters
   * @return List of Object arrays with rule metadata (excluding embeddings)
   */
  @Query(
      "SELECT r.ruleId, r.artifactId, r.componentId, r.subjectId, r.domainId, "
          + "r.ruleName, r.ruleType, r.conditionText, r.outcomeText, r.priority, "
          + "r.confidence, r.createdAt "
          + "FROM KnowledgeBusinessRuleEntity r ORDER BY r.confidence DESC")
  List<Object[]> findAllWithoutEmbedding(Pageable pageable);

  /**
   * Finds business rules of a specific type without embedding data.
   *
   * <p>Combines type filtering with lightweight projection for efficient queries.
   *
   * @param ruleType Type of rule to filter by
   * @param pageable Pagination parameters
   * @return List of Object arrays with rule metadata (excluding embeddings)
   */
  @Query(
      "SELECT r.ruleId, r.artifactId, r.componentId, r.subjectId, r.domainId, "
          + "r.ruleName, r.ruleType, r.conditionText, r.outcomeText, r.priority, "
          + "r.confidence, r.createdAt "
          + "FROM KnowledgeBusinessRuleEntity r WHERE r.ruleType = :ruleType "
          + "ORDER BY r.confidence DESC")
  List<Object[]> findByRuleTypeWithoutEmbedding(String ruleType, Pageable pageable);

  /**
   * Deletes all business rules belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of rules deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Deletes all business rules associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of rules deleted
   */
  int deleteByArtifactId(UUID artifactId);

  /**
   * Counts total business rules for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of business rules
   */
  long countBySubjectId(UUID subjectId);

  /**
   * Finds semantically similar business rules using vector similarity search.
   *
   * <p>Uses PostgreSQL pgvector's cosine distance operator to find rules with embeddings most
   * similar to the query embedding. Results are ordered by similarity (highest first).
   *
   * <p>Useful for finding rules related to a specific business scenario, condition, or outcome.
   *
   * @param subjectId Subject UUID (required)
   * @param embedding Query embedding in pgvector string format: "[0.1,0.2,0.3,...]"
   * @param domainId Optional domain UUID filter (null = all domains)
   * @param limit Maximum number of results to return
   * @return List of Object arrays containing: [rule_id, artifact_id, component_id, subject_id,
   *     domain_id, rule_name, rule_type, condition_text, outcome_text, priority, confidence,
   *     similarity_score]
   */
  @Query(
      value =
          "SELECT rule_id, artifact_id, component_id, subject_id, domain_id, "
              + "rule_name, rule_type, condition_text, outcome_text, priority, "
              + "confidence, "
              + "(1 - (embedding <=> CAST(:embedding AS vector))) as similarity "
              + "FROM knowledge.knowledge_business_rules "
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
