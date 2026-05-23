package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeBusinessRuleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeBusinessRuleRepository
    extends JpaRepository<KnowledgeBusinessRuleEntity, UUID> {

  Page<KnowledgeBusinessRuleEntity> findByRuleTypeOrderByConfidenceDesc(
      String ruleType, Pageable pageable);

  @Query(
      "SELECT r.ruleId, r.artifactId, r.componentId, r.projectId, r.domainId, "
          + "r.ruleName, r.ruleType, r.conditionText, r.outcomeText, r.priority, "
          + "r.confidence, r.createdAt "
          + "FROM KnowledgeBusinessRuleEntity r ORDER BY r.confidence DESC")
  List<Object[]> findAllWithoutEmbedding(Pageable pageable);

  @Query(
      "SELECT r.ruleId, r.artifactId, r.componentId, r.projectId, r.domainId, "
          + "r.ruleName, r.ruleType, r.conditionText, r.outcomeText, r.priority, "
          + "r.confidence, r.createdAt "
          + "FROM KnowledgeBusinessRuleEntity r WHERE r.ruleType = :ruleType "
          + "ORDER BY r.confidence DESC")
  List<Object[]> findByRuleTypeWithoutEmbedding(String ruleType, Pageable pageable);
}
