package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.SemanticChunkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SemanticChunkRepository
    extends JpaRepository<SemanticChunkEntity, String>,
        JpaSpecificationExecutor<SemanticChunkEntity> {

  List<SemanticChunkEntity> findBySubjectIdAndSourceObjectOrderByChunkIndex(
      UUID subjectId, String sourceObject);

  int deleteBySubjectId(UUID subjectId);

  long countBySubjectId(UUID subjectId);

  @Query(
      value =
          "SELECT chunk_id, artifact_id, subject_id, source_object, domain, "
              + "chunk_index, content, chunk_content_hash, "
              + "(1 - (embedding <=> CAST(:embedding AS vector))) as similarity "
              + "FROM semantic_chunks "
              + "WHERE subject_id = :subjectId "
              + "AND (:sourceObject IS NULL OR source_object = :sourceObject) "
              + "AND (:domain IS NULL OR domain = :domain) "
              + "AND embedding IS NOT NULL "
              + "ORDER BY embedding <=> CAST(:embedding AS vector) "
              + "LIMIT :limit",
      nativeQuery = true)
  List<Object[]> findSimilarByEmbedding(
      @Param("subjectId") UUID subjectId,
      @Param("embedding") String embedding,
      @Param("sourceObject") String sourceObject,
      @Param("domain") String domain,
      @Param("limit") int limit);
}
