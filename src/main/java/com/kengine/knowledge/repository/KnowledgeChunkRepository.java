package com.kengine.knowledge.repository;

import com.kengine.knowledge.entity.KnowledgeChunkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, UUID> {
  List<KnowledgeChunkEntity> findTop20ByProjectIdAndContentContainingIgnoreCase(
      UUID projectId, String query);

  List<KnowledgeChunkEntity> findByProjectIdAndEntityType(UUID projectId, String entityType);

  List<KnowledgeChunkEntity> findByProjectId(UUID projectId);

  int deleteByProjectId(UUID projectId);

  @Query(
      value =
          "select * from knowledge.knowledge_chunks "
              + "where project_id = :projectId and embedding is not null "
              + "order by embedding <=> cast(:embedding as vector) limit :limit",
      nativeQuery = true)
  List<KnowledgeChunkEntity> searchByEmbedding(
      @Param("projectId") UUID projectId,
      @Param("embedding") String embedding,
      @Param("limit") int limit);
}
