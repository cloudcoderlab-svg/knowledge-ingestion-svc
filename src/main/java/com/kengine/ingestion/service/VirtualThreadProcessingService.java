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
import java.util.concurrent.atomic.AtomicInteger;
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

    log.info("Starting concurrent processing of {} files for process {}", files.size(), processId);

    // Track success/failure counts
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    // Process all files concurrently using virtual threads
    List<CompletableFuture<Void>> futures =
        files.stream()
            .map(
                file ->
                    CompletableFuture.runAsync(
                        () -> processFile(processId, file, subject, successCount, failureCount),
                        virtualThreadExecutor))
            .toList();

    // Wait for all files to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .whenComplete(
            (result, throwable) ->
                updateFinalStatus(processId, successCount.get(), failureCount.get(), throwable));
  }

  /**
   * Processes a single file.
   *
   * @param processId Process UUID
   * @param file File path
   * @param subject Subject entity
   * @param successCount Success counter
   * @param failureCount Failure counter
   */
  private void processFile(
      UUID processId,
      String file,
      SubjectEntity subject,
      AtomicInteger successCount,
      AtomicInteger failureCount) {
    try {
      log.info("Processing file: {} for process: {}", file, processId);

      // Update current file
      processTrackingService.updateProgress(
          processId, successCount.get(), failureCount.get(), file);

      // Ingest the file
      knowledgeIngestionService.ingestFromGcs(bucketName, file);

      // Mark as success
      int currentSuccess = successCount.incrementAndGet();
      processTrackingService.markFileProcessed(processId, file, true, null);

      log.info(
          "Successfully processed file: {} ({}/{} completed)",
          file,
          currentSuccess + failureCount.get(),
          currentSuccess + failureCount.get());

    } catch (Exception e) {
      log.error("Failed to process file: {} - {}", file, e.getMessage(), e);

      // Mark as failed
      int currentFailure = failureCount.incrementAndGet();
      processTrackingService.markFileProcessed(processId, file, false, e.getMessage());

      log.warn("Failed to process file: {} ({} failures so far)", file, currentFailure);
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
      } else {
        log.info("Processing completed successfully for process: {}", processId);
        processTrackingService.updateStatus(processId, ProcessStatus.SUCCESS, null);
      }
    } catch (Exception e) {
      log.error("Error updating final status for process: {}", processId, e);
    }
  }
}
