package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.ArtifactEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing artifact entities.
 *
 * <p>Artifacts represent source documents stored in GCS that have been processed and ingested. Each
 * artifact has a unique combination of subject, bucket, object path, and content hash.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on artifacts
 *   <li>Deduplication using content hash
 *   <li>Lookup by GCS location and hash
 *   <li>Bulk deletion by subject
 * </ul>
 */
@Repository
public interface ArtifactRepository
    extends JpaRepository<ArtifactEntity, String>, JpaSpecificationExecutor<ArtifactEntity> {
  /**
   * Finds an artifact by its unique combination of subject, GCS location, and content hash.
   *
   * <p>Used for deduplication to avoid re-processing identical documents.
   *
   * @param subjectId Subject UUID
   * @param sourceBucket GCS bucket name
   * @param sourceObject GCS object path
   * @param contentHash SHA-256 hash of document content
   * @return Optional containing the artifact if found
   */
  Optional<ArtifactEntity> findBySubjectIdAndSourceBucketAndSourceObjectAndContentHash(
      UUID subjectId, String sourceBucket, String sourceObject, String contentHash);

  /**
   * Finds all artifacts for a subject.
   *
   * @param subjectId Subject UUID
   * @return List of all artifacts for the subject
   */
  List<ArtifactEntity> findBySubjectId(UUID subjectId);

  /**
   * Deletes all artifacts belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of artifacts deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Counts total artifacts for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of artifacts
   */
  long countBySubjectId(UUID subjectId);
}
