package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.IngestionDocumentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing ingestion document entities.
 *
 * <p>Ingestion documents represent the raw parsed content and metadata extracted from source files
 * during document processing.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on ingestion documents
 *   <li>Deduplication checks using content hash
 *   <li>Bulk deletion by subject or artifact
 * </ul>
 */
@Repository
public interface IngestionDocumentRepository
    extends JpaRepository<IngestionDocumentEntity, String> {
  /**
   * Checks if a document with the given subject, location, and hash already exists.
   *
   * <p>Used during ingestion to avoid processing duplicate documents.
   *
   * @param subjectId Subject UUID
   * @param sourceBucket GCS bucket name
   * @param sourceObject GCS object path
   * @param contentHash SHA-256 hash of document content
   * @return true if document exists, false otherwise
   */
  boolean existsBySubjectIdAndSourceBucketAndSourceObjectAndContentHash(
      UUID subjectId, String sourceBucket, String sourceObject, String contentHash);

  /**
   * Deletes all ingestion documents belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of documents deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Deletes all ingestion documents associated with a specific artifact.
   *
   * @param artifactId Artifact UUID
   * @return Number of documents deleted
   */
  int deleteByArtifactId(UUID artifactId);

  /**
   * Counts total ingestion documents for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of documents
   */
  long countBySubjectId(UUID subjectId);
}
