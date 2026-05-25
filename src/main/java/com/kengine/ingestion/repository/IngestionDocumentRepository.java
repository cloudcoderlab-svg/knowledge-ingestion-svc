package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.IngestionDocumentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestionDocumentRepository
    extends JpaRepository<IngestionDocumentEntity, String> {
  boolean existsBySubjectIdAndSourceBucketAndSourceObjectAndContentHash(
      UUID subjectId, String sourceBucket, String sourceObject, String contentHash);

  int deleteBySubjectId(UUID subjectId);

  long countBySubjectId(UUID subjectId);
}
