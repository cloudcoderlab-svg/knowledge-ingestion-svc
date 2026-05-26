package com.kengine.ingestion.service;

import com.kengine.ingestion.entity.KnowledgeIngestionDocumentProcessEntity;
import com.kengine.ingestion.model.ProcessStatus;
import com.kengine.ingestion.repository.KnowledgeIngestionDocProcessRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for tracking individual document processing status. */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIngestionProcessService {

  private final KnowledgeIngestionDocProcessRepository repository;

  /**
   * Creates a document tracking record in PENDING status.
   *
   * @param processId the parent process ID
   * @param subjectId the subject ID
   * @param filePath the GCS file path
   * @return the created tracking entity
   */
  @Transactional
  public KnowledgeIngestionDocumentProcessEntity createDocumentTracking(
      UUID processId, UUID subjectId, String filePath) {
    String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);

    KnowledgeIngestionDocumentProcessEntity tracking =
        KnowledgeIngestionDocumentProcessEntity.builder()
            .processId(processId)
            .subjectId(subjectId)
            .filePath(filePath)
            .fileName(fileName)
            .status(ProcessStatus.INIT)
            .build();

    return repository.save(tracking);
  }

  /**
   * Marks a document as in-progress.
   *
   * @param docProcessId the document process ID
   */
  @Transactional
  public void markDocumentStarted(UUID docProcessId) {
    KnowledgeIngestionDocumentProcessEntity tracking =
        repository
            .findById(docProcessId)
            .orElseThrow(() -> new IllegalArgumentException("Document tracking not found"));

    tracking.setStatus(ProcessStatus.IN_PROGRESS);
    tracking.setStartedAt(OffsetDateTime.now());

    repository.save(tracking);
    log.info(
        "[DocProcess] Started processing: {} (doc_process_id: {})",
        tracking.getFileName(),
        docProcessId);
  }

  /**
   * Marks a document as successfully completed.
   *
   * @param docProcessId the document process ID
   * @param artifactId the created artifact ID
   */
  @Transactional
  public void markDocumentSuccess(UUID docProcessId, UUID artifactId) {

    KnowledgeIngestionDocumentProcessEntity tracking =
        repository
            .findById(docProcessId)
            .orElseThrow(() -> new IllegalArgumentException("Document tracking not found"));

    OffsetDateTime completedAt = OffsetDateTime.now();
    long durationMs = 0;
    if (tracking.getStartedAt() != null) {
      durationMs =
          completedAt.toInstant().toEpochMilli()
              - tracking.getStartedAt().toInstant().toEpochMilli();
    }

    tracking.setStatus(ProcessStatus.SUCCESS);
    tracking.setArtifactId(artifactId);
    tracking.setCompletedAt(completedAt);
    tracking.setDurationMs(durationMs);

    repository.save(tracking);
    log.info(
        "[DocProcess] Successfully completed: {} (doc_process_id: {}, duration: {} ms)",
        tracking.getFileName(),
        tracking.getDocProcessId(),
        durationMs);
  }

  /**
   * Marks a document as failed with error details.
   *
   * @param docProcessId the document process ID
   * @param error the exception that caused the failure
   */
  @Transactional
  public void markDocumentFailed(UUID docProcessId, Exception error) {
    KnowledgeIngestionDocumentProcessEntity tracking =
        repository
            .findById(docProcessId)
            .orElseThrow(() -> new IllegalArgumentException("Document tracking not found"));

    OffsetDateTime completedAt = OffsetDateTime.now();
    long durationMs = 0;
    if (tracking.getStartedAt() != null) {
      durationMs =
          completedAt.toInstant().toEpochMilli()
              - tracking.getStartedAt().toInstant().toEpochMilli();
    }

    tracking.setStatus(ProcessStatus.FAILED);
    tracking.setErrorMessage(error.getMessage());
    tracking.setCompletedAt(completedAt);
    tracking.setDurationMs(durationMs);

    repository.save(tracking);
    log.error(
        "[DocProcess] Failed: {} (doc_process_id: {}, duration: {} ms)",
        tracking.getFileName(),
        tracking.getDocProcessId(),
        durationMs,
        error);
  }

  /**
   * Gets all document tracking records for a process.
   *
   * @param processId the process ID
   * @return list of document tracking records
   */
  public List<KnowledgeIngestionDocumentProcessEntity> getDocumentTrackings(UUID processId) {
    return repository.findByProcessId(processId);
  }

  /**
   * Checks if all documents in a process are completed (success or failed).
   *
   * @param processId the process ID
   * @return true if all documents are completed
   */
  public boolean areAllDocumentsCompleted(UUID processId) {
    return !repository.hasIncompleteDocs(processId);
  }

  /**
   * Gets counts of documents by status.
   *
   * @param processId the process ID
   * @return array [success, failed, inProgress, pending]
   */
  public long[] getDocumentStatusCounts(UUID processId) {
    long successCount = repository.countByProcessIdAndStatus(processId, ProcessStatus.SUCCESS);
    long failedCount = repository.countByProcessIdAndStatus(processId, ProcessStatus.FAILED);
    long inProgressCount =
        repository.countByProcessIdAndStatus(processId, ProcessStatus.IN_PROGRESS);
    long pendingCount = repository.countByProcessIdAndStatus(processId, ProcessStatus.INIT);

    return new long[] {successCount, failedCount, inProgressCount, pendingCount};
  }
}
