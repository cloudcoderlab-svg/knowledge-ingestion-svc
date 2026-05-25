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

@Repository
public interface KnowledgeBusinessRuleRepository
    extends JpaRepository<KnowledgeBusinessRuleEntity, UUID> {

  Page<KnowledgeBusinessRuleEntity> findByRuleTypeOrderByConfidenceDesc(
      String ruleType, Pageable pageable);

  @Query(
      "SELECT r.ruleId, r.artifactId, r.componentId, r.subjectId, r.domainId, "
          + "r.ruleName, r.ruleType, r.conditionText, r.outcomeText, r.priority, "
          + "r.confidence, r.createdAt "
          + "FROM KnowledgeBusinessRuleEntity r ORDER BY r.confidence DESC")
  List<Object[]> findAllWithoutEmbedding(Pageable pageable);

  @Query(
      "SELECT r.ruleId, r.artifactId, r.componentId, r.subjectId, r.domainId, "
          + "r.ruleName, r.ruleType, r.conditionText, r.outcomeText, r.priority, "
          + "r.confidence, r.createdAt "
          + "FROM KnowledgeBusinessRuleEntity r WHERE r.ruleType = :ruleType "
          + "ORDER BY r.confidence DESC")
  List<Object[]> findByRuleTypeWithoutEmbedding(String ruleType, Pageable pageable);

  int deleteBySubjectId(UUID subjectId);

  int deleteByArtifactId(UUID artifactId);

  long countBySubjectId(UUID subjectId);

  @Query(
      value =
          "SELECT rule_id, artifact_id, component_id, subject_id, domain_id, "
              + "rule_name, rule_type, condition_text, outcome_text, priority, "
              + "confidence, "
              + "(1 - (embedding <=> CAST(:embedding AS vector))) as similarity "
              + "FROM knowledge_business_rules "
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
