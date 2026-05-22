package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.IngestionDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestionDocumentRepository
    extends JpaRepository<IngestionDocumentEntity, String> {
  boolean existsByProjectIdAndSourceBucketAndSourceObjectAndContentHash(
      String projectId, String sourceBucket, String sourceObject, String contentHash);
}
