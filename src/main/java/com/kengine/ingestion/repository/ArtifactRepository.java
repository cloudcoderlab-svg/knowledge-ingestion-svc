package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.ArtifactEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository
    extends JpaRepository<ArtifactEntity, String>, JpaSpecificationExecutor<ArtifactEntity> {
  Optional<ArtifactEntity> findByProjectIdAndSourceBucketAndSourceObjectAndContentHash(
      String projectId, String sourceBucket, String sourceObject, String contentHash);
}
