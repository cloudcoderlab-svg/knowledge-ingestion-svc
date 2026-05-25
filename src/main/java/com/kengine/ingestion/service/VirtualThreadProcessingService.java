package com.kengine.ingestion.service;

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
          }
        },
        virtualThreadExecutor);
  }

  /**
   * Processes all files in a process concurrently using virtual threads.
   *
   * @param processId Process UUID
   */
  private void processFiles(UUID processId) {
    ProcessTrackingEntity process = processTrackingService.getProcessEntity(processId);

    // Update status to IN_PROGRESS
    processTrackingService.updateStatus(processId, ProcessStatus.IN_PROGRESS, null);

    List<String> files = process.getFileList();
    if (files == null || files.isEmpty()) {
      log.warn("No files to process for process: {}", processId);
      processTrackingService.updateStatus(processId, ProcessStatus.SUCCESS, null);
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

    log.info("Starting sequential processing of {} files for process {}", files.size(), processId);

    // Track success/failure counts
    int successCount = 0;
    int failureCount = 0;

    // Process all files sequentially to avoid optimistic lock conflicts
    for (String file : files) {
      try {
        log.info("Processing file: {} for process: {}", file, processId);

        // Update current file
        processTrackingService.updateProgress(processId, successCount, failureCount, file);

        // Ingest the file
        knowledgeIngestionService.ingestFromGcs(bucketName, file);

        // Mark as success
        successCount++;
        processTrackingService.markFileProcessed(processId, file, true, null);

        log.info(
            "Successfully processed file: {} ({}/{} completed)",
            file,
            successCount + failureCount,
            files.size());

      } catch (Exception e) {
        log.error("Failed to process file: {} - {}", file, e.getMessage(), e);

        // Mark as failed
        failureCount++;
        processTrackingService.markFileProcessed(processId, file, false, e.getMessage());

        log.warn("Failed to process file: {} ({} failures so far)", file, failureCount);
      }
    }

    // Update final status after all files are processed
    updateFinalStatus(processId, successCount, failureCount, null);
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
      if (throwable != null) {
        log.error("Processing completed with errors for process: {}", processId, throwable);
        processTrackingService.updateStatus(
            processId, ProcessStatus.FAILED, "Processing failed: " + throwable.getMessage());
      } else if (failureCount > 0 && successCount == 0) {
        log.warn("All files failed for process: {}", processId);
        processTrackingService.updateStatus(processId, ProcessStatus.FAILED, "All files failed");
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

        // Trigger cross-document relationship analysis for successful ingestion
        triggerCrossDocumentAnalysis(processId);
      } else {
        log.info("Processing completed successfully for process: {}", processId);
        processTrackingService.updateStatus(processId, ProcessStatus.SUCCESS, null);

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
          "Triggering cross-document relationship analysis for subject: {} ({})",
          subject.getSubjectName(),
          subjectId);

      // Run cross-document analysis asynchronously in a virtual thread
      CompletableFuture.runAsync(
          () -> {
            try {
              crossDocumentRelationshipService.analyzeAndInferRelationships(
                  subjectId, subject.getSubjectName());
              log.info(
                  "Cross-document relationship analysis completed for subject: {}",
                  subject.getSubjectName());
            } catch (Exception e) {
              log.error(
                  "Error during cross-document relationship analysis for subject: {}",
                  subject.getSubjectName(),
                  e);
            }
          },
          virtualThreadExecutor);

    } catch (Exception e) {
      log.error(
          "Error triggering cross-document relationship analysis for process: {}", processId, e);
    }
  }
}
