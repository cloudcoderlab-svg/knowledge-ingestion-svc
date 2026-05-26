package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeIngestionDocumentProcessEntity;
import com.kengine.ingestion.model.ProcessStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing knowledge ingestion document process entities.
 *
 * <p>Tracks individual document processing tasks within a batch ingestion process. Each entity
 * represents a single file being processed with its own status, metrics, and error information.
 *
 * <p>Used for parallel processing coordination and progress tracking.
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on document process entities
 *   <li>Query by process ID, file path, and status
 *   <li>Aggregate status counting
 *   <li>Completion detection
 * </ul>
 */
@Repository
public interface KnowledgeIngestionDocProcessRepository
    extends JpaRepository<KnowledgeIngestionDocumentProcessEntity, UUID> {

  /**
   * Finds all document process records for a given process.
   *
   * @param processId Process UUID
   * @return List of all document process records in the batch
   */
  List<KnowledgeIngestionDocumentProcessEntity> findByProcessId(UUID processId);

  /**
   * Finds a specific document process record by process ID and file path.
   *
   * @param processId Process UUID
   * @param filePath GCS file path
   * @return Optional containing the document process record if found
   */
  Optional<KnowledgeIngestionDocumentProcessEntity> findByProcessIdAndFilePath(
      UUID processId, String filePath);

  /**
   * Counts documents with a specific status within a process.
   *
   * @param processId Process UUID
   * @param status Process status (INIT, IN_PROGRESS, SUCCESS, FAILED)
   * @return Count of documents with the specified status
   */
  long countByProcessIdAndStatus(UUID processId, ProcessStatus status);

  /**
   * Counts documents with any of the specified statuses within a process.
   *
   * <p>Useful for aggregate queries like counting all active documents (IN_PROGRESS + INIT).
   *
   * @param processId Process UUID
   * @param statuses List of statuses to count
   * @return Count of documents matching any of the statuses
   */
  @Query(
      "SELECT COUNT(d) FROM KnowledgeIngestionDocumentProcessEntity d WHERE d.processId = :processId AND d.status IN :statuses")
  long countByProcessIdAndStatusIn(
      @Param("processId") UUID processId, @Param("statuses") List<ProcessStatus> statuses);

  /**
   * Finds all documents with a specific status within a process.
   *
   * @param processId Process UUID
   * @param status Process status to filter by
   * @return List of documents with the specified status
   */
  @Query(
      "SELECT d FROM KnowledgeIngestionDocumentProcessEntity d WHERE d.processId = :processId AND d.status = :status")
  List<KnowledgeIngestionDocumentProcessEntity> findByProcessIdAndStatus(
      @Param("processId") UUID processId, @Param("status") ProcessStatus status);

  /**
   * Checks if a process has any incomplete documents.
   *
   * <p>Returns true if any documents are still in INIT, NEW, or IN_PROGRESS status. Used to
   * determine if the overall process should be marked as complete.
   *
   * @param processId Process UUID
   * @return true if incomplete documents exist, false otherwise
   */
  @Query(
      "SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM KnowledgeIngestionDocumentProcessEntity d "
          + "WHERE d.processId = :processId AND d.status IN ('INIT', 'NEW', 'IN_PROGRESS')")
  boolean hasIncompleteDocs(@Param("processId") UUID processId);
}
