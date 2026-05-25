package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.ArtifactEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository
    extends JpaRepository<ArtifactEntity, String>, JpaSpecificationExecutor<ArtifactEntity> {
  Optional<ArtifactEntity> findBySubjectIdAndSourceBucketAndSourceObjectAndContentHash(
      UUID subjectId, String sourceBucket, String sourceObject, String contentHash);

  List<ArtifactEntity> findBySubjectId(UUID subjectId);

  int deleteBySubjectId(UUID subjectId);

  long countBySubjectId(UUID subjectId);
}
