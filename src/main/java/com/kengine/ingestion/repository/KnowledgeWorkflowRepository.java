package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeWorkflowEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeWorkflowRepository extends JpaRepository<KnowledgeWorkflowEntity, UUID> {
  int deleteBySubjectId(UUID subjectId);

  long countBySubjectId(UUID subjectId);

  @Query(
      value =
          "SELECT workflow_id, artifact_id, subject_id, domain_id, workflow_name, "
              + "trigger_text, outcome_text, owner, confidence, "
              + "(1 - (embedding <=> CAST(:embedding AS vector))) as similarity "
              + "FROM knowledge_workflows "
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
