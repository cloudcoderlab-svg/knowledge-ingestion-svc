package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.ClassificationResult;
import com.kengine.ingestion.entity.KnowledgeIngestionDocumentProcessEntity;
import com.kengine.ingestion.entity.ProcessTrackingEntity;
import com.kengine.ingestion.entity.SubjectEntity;
import com.kengine.ingestion.model.ProcessStatus;
import com.kengine.ingestion.repository.SubjectRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirtualThreadProcessingService {

  private final ProcessTrackingService processTrackingService;
  private final KnowledgeIngestionService knowledgeIngestionService;
  private final GcsFolderManagementService gcsFolderManagementService;
  private final SubjectRepository subjectRepository;
  private final PostgresStorageService storageService;
  private final CrossDocumentRelationshipService crossDocumentRelationshipService;
  private final KnowledgeIngestionProcessService docProcessService;

  @Value("${gcp.storage.bucket-name}")
  private String bucketName;

  private ExecutorService virtualThreadExecutor;

  @PostConstruct
  public void init() {
    virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    log.info("Initialized virtual thread executor for concurrent file processing");
  }

  @PreDestroy
  public void shutdown() {
    if (virtualThreadExecutor != null) {
      virtualThreadExecutor.shutdown();
      log.info("Shut down virtual thread executor");
    }
  }

  /**
   * Starts processing files for a process using virtual threads.
   *
   * @param processId Process UUID
   */
  public void startProcessing(UUID processId) {
    // Run the processing asynchronously in a virtual thread
    CompletableFuture.runAsync(
        () -> {
          try {
            processFiles(processId);
          } catch (Exception e) {
            log.error(
                "Error in processing orchestration for process {}: {}",
                processId,
                e.getMessage(),
                e);
            processTrackingService.updateStatus(
                processId,
                ProcessStatus.FAILED,
                "Processing orchestration failed: " + e.getMessage());

            // Mark subject as FAILED
            try {
              ProcessTrackingEntity process = processTrackingService.getProcessEntity(processId);
              updateSubjectStatus(
                  process.getSubjectId(), com.kengine.ingestion.entity.SubjectStatus.FAILED);
            } catch (Exception ex) {
              log.error("Failed to update subject status for failed process: {}", processId, ex);
            }
          }
        },
        virtualThreadExecutor);
  }

  /**
   * Processes all files in a process concurrently using virtual threads. Each document has its own
   * tracking row to avoid optimistic lock conflicts.
   *
   * @param processId Process UUID
   */
  private void processFiles(UUID processId) {
    ProcessTrackingEntity process = processTrackingService.getProcessEntity(processId);

    // Update status to IN_PROGRESS
    processTrackingService.updateStatus(processId, ProcessStatus.IN_PROGRESS, null);

    // Update subject status to INGESTING
    updateSubjectStatus(
        process.getSubjectId(), com.kengine.ingestion.entity.SubjectStatus.INGESTING);

    List<String> files = process.getFileList();
    if (files == null || files.isEmpty()) {
      log.warn("No files to process for process: {}", processId);
      processTrackingService.updateStatus(processId, ProcessStatus.SUCCESS, null);
      // Keep subject in DRAFT status when no files are uploaded
      updateSubjectStatus(process.getSubjectId(), com.kengine.ingestion.entity.SubjectStatus.DRAFT);
      return;
    }

    // Get subject details
    SubjectEntity subject =
        subjectRepository
            .findById(process.getSubjectId())
            .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

    // STEP 1: Clean up duplicate artifacts before processing
    log.info("Cleaning up duplicate artifacts for subject: {}", process.getSubjectId());
    try {
      int deletedDuplicates = storageService.cleanupDuplicateArtifacts(process.getSubjectId());
      log.info("Cleanup completed. Deleted {} duplicate artifacts", deletedDuplicates);
    } catch (Exception e) {
      log.error(
          "Error during duplicate cleanup: {}. Continuing with processing.", e.getMessage(), e);
    }

    // STEP 2: Create document process tracking records for all files
    log.info(
        "[ParallelIngestion] Creating {} document process tracking records for process {}",
        files.size(),
        processId);
    List<KnowledgeIngestionDocumentProcessEntity> docTrackings =
        files.stream()
            .map(
                file ->
                    docProcessService.createDocumentTracking(
                        processId, process.getSubjectId(), file))
            .toList();

    log.info(
        "[ParallelIngestion] Starting PARALLEL processing of {} documents using virtual threads (process: {})",
        files.size(),
        processId);

    // STEP 3: Process all documents in parallel using virtual threads
    List<CompletableFuture<Void>> futures =
        docTrackings.stream()
            .map(
                docTracking ->
                    CompletableFuture.runAsync(
                        () -> processDocument(docTracking), virtualThreadExecutor))
            .toList();

    // STEP 4: Wait for all documents to complete
    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      log.info(
          "[ParallelIngestion] All {} documents completed processing (process: {})",
          files.size(),
          processId);
    } catch (Exception e) {
      log.error("Error waiting for document processing to complete: {}", e.getMessage(), e);
    }

    // STEP 5: Get final counts and update process status
    long[] counts = docProcessService.getDocumentStatusCounts(processId);
    long successCount = counts[0];
    long failureCount = counts[1];

    log.info(
        "[ParallelIngestion] Process {} completed: {}/{} successful, {}/{} failed",
        processId,
        successCount,
        files.size(),
        failureCount,
        files.size());

    // Update final status
    updateFinalStatus(processId, (int) successCount, (int) failureCount, null);
  }

  /**
   * Processes a single document and updates its tracking status.
   *
   * @param docTracking the document tracking entity
   */
  private void processDocument(KnowledgeIngestionDocumentProcessEntity docTracking) {
    UUID docProcessId = docTracking.getDocProcessId();
    String filePath = docTracking.getFilePath();
    String fileName = docTracking.getFileName();

    try {
      // Mark as started
      docProcessService.markDocumentStarted(docProcessId);

      log.info(
          "[DocProcess] Processing document: {} (process: {}, doc_process_id: {})",
          fileName,
          docTracking.getProcessId(),
          docProcessId);

      // Ingest the document
      ClassificationResult result = knowledgeIngestionService.ingestFromGcs(bucketName, filePath);
      UUID artifactId = result.getArtifactId();

      // Mark as success
      docProcessService.markDocumentSuccess(docProcessId, artifactId);

      log.info(
          "[DocProcess] Successfully processed: {} (doc_process_id: {})", fileName, docProcessId);

    } catch (Exception e) {
      log.error(
          "[DocProcess] Failed to process: {} (doc_process_id: {})", fileName, docProcessId, e);

      // Mark as failed with error details
      docProcessService.markDocumentFailed(docProcessId, e);
    }
  }

  /**
   * Updates the final status of the process after all files are processed.
   *
   * @param processId Process UUID
   * @param successCount Number of successfully processed files
   * @param failureCount Number of failed files
   * @param throwable Any throwable from the overall processing
   */
  private void updateFinalStatus(
      UUID processId, int successCount, int failureCount, Throwable throwable) {
    try {
      ProcessTrackingEntity process = processTrackingService.getProcessEntity(processId);
      UUID subjectId = process.getSubjectId();

      if (throwable != null) {
        log.error("Processing completed with errors for process: {}", processId, throwable);
        processTrackingService.updateStatus(
            processId, ProcessStatus.FAILED, "Processing failed: " + throwable.getMessage());
        // Mark subject as FAILED
        updateSubjectStatus(subjectId, com.kengine.ingestion.entity.SubjectStatus.FAILED);
      } else if (failureCount > 0 && successCount == 0) {
        log.warn("All files failed for process: {}", processId);
        processTrackingService.updateStatus(processId, ProcessStatus.FAILED, "All files failed");
        // Mark subject as FAILED
        updateSubjectStatus(subjectId, com.kengine.ingestion.entity.SubjectStatus.FAILED);
      } else if (failureCount > 0) {
        log.warn(
            "Processing completed with partial failures for process: {} ({} success, {} failed)",
            processId,
            successCount,
            failureCount);
        processTrackingService.updateStatus(
            processId,
            ProcessStatus.SUCCESS,
            String.format(
                "Completed with %d failures out of %d files",
                failureCount, successCount + failureCount));

        // Mark subject as ACTIVE even with partial failures
        updateSubjectStatus(subjectId, com.kengine.ingestion.entity.SubjectStatus.ACTIVE);

        // Trigger cross-document relationship analysis for successful ingestion
        triggerCrossDocumentAnalysis(processId);
      } else {
        log.info("Processing completed successfully for process: {}", processId);
        processTrackingService.updateStatus(processId, ProcessStatus.SUCCESS, null);

        // Mark subject as ACTIVE
        updateSubjectStatus(subjectId, com.kengine.ingestion.entity.SubjectStatus.ACTIVE);

        // Trigger cross-document relationship analysis after successful ingestion
        triggerCrossDocumentAnalysis(processId);
      }
    } catch (Exception e) {
      log.error("Error updating final status for process: {}", processId, e);
    }
  }

  /**
   * Triggers cross-document relationship analysis for the subject associated with the process.
   *
   * @param processId Process UUID
   */
  private void triggerCrossDocumentAnalysis(UUID processId) {
    try {
      // Get the process to retrieve subject information
      ProcessTrackingEntity process = processTrackingService.getProcessEntity(processId);
      UUID subjectId = process.getSubjectId();

      // Get subject details
      SubjectEntity subject =
          subjectRepository
              .findById(subjectId)
              .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

      log.info(
          "[CrossDocAnalysis] Triggering cross-document relationship analysis for subject: {} (subject_id: {}, process: {})",
          subject.getSubjectName(),
          subjectId,
          processId);

      // Run cross-document analysis asynchronously in a virtual thread
      CompletableFuture.runAsync(
          () -> {
            try {
              crossDocumentRelationshipService.analyzeAndInferRelationships(
                  subjectId, subject.getSubjectName());
              log.info(
                  "[CrossDocAnalysis] Completed for subject: {} (subject_id: {})",
                  subject.getSubjectName(),
                  subjectId);
            } catch (Exception e) {
              log.error(
                  "[CrossDocAnalysis] Failed for subject: {} (subject_id: {})",
                  subject.getSubjectName(),
                  subjectId,
                  e);
            }
          },
          virtualThreadExecutor);

    } catch (Exception e) {
      log.error(
          "Error triggering cross-document relationship analysis for process: {}", processId, e);
    }
  }

  /**
   * Gets the document processing status for a process.
   *
   * @param processId Process UUID
   * @return List of document tracking entities
   */
  public List<KnowledgeIngestionDocumentProcessEntity> getDocumentStatus(UUID processId) {
    return docProcessService.getDocumentTrackings(processId);
  }

  /**
   * Updates the status of a subject.
   *
   * @param subjectId Subject UUID
   * @param status New status
   */
  private void updateSubjectStatus(
      UUID subjectId, com.kengine.ingestion.entity.SubjectStatus status) {
    try {
      SubjectEntity subject =
          subjectRepository
              .findById(subjectId)
              .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
      subject.setStatus(status);
      subjectRepository.save(subject);
      log.info("Updated subject {} status to: {}", subjectId, status);
    } catch (Exception e) {
      log.error(
          "Failed to update subject {} status to {}: {}", subjectId, status, e.getMessage(), e);
    }
  }
}
