package com.kengine.knowledge.service;

import com.kengine.knowledge.entity.ProcessTrackingEntity;
import com.kengine.knowledge.ingestion.service.KnowledgeIngestionService;
import com.kengine.knowledge.ingestion.service.ProjectKnowledgeResetService;
import com.kengine.knowledge.repository.ProcessTrackingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestionProcessRunner {
  private final ProcessTrackingRepository processRepository;
  private final ProjectKnowledgeResetService projectKnowledgeResetService;
  private final KnowledgeIngestionService ingestionService;
  private final Executor documentExecutor;

  public IngestionProcessRunner(
      ProcessTrackingRepository processRepository,
      ProjectKnowledgeResetService projectKnowledgeResetService,
      KnowledgeIngestionService ingestionService,
      @Qualifier("ingestionDocumentExecutor") Executor documentExecutor) {
    this.processRepository = processRepository;
    this.projectKnowledgeResetService = projectKnowledgeResetService;
    this.ingestionService = ingestionService;
    this.documentExecutor = documentExecutor;
  }

  @Async("ingestionProcessExecutor")
  public void runIngestion(UUID processId, UUID projectId, String bucketName, List<String> files) {
    AtomicInteger processed = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();
    try {
      projectKnowledgeResetService.resetIngestionData(projectId);
      updateProcess(
          processId,
          process -> {
            process.setStatus("RUNNING");
            process.setProcessedFiles(0);
            process.setFailedFiles(0);
            process.setFailureCause(null);
          });

      List<CompletableFuture<Void>> futures =
          files.stream()
              .map(
                  file ->
                      CompletableFuture.runAsync(
                          () -> processFile(processId, bucketName, file, processed, failed),
                          documentExecutor))
              .toList();

      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
      int failedCount = failed.get();
      updateProcess(
          processId,
          process -> {
            process.setProcessedFiles(processed.get());
            process.setFailedFiles(failedCount);
            process.setCurrentFile(null);
            process.setStatus(failedCount == 0 ? "COMPLETED" : "PARTIAL_SUCCESS");
            process.setCompletedAt(OffsetDateTime.now());
          });
    } catch (Exception e) {
      log.error("Ingestion process failed: {}", processId, e);
      updateProcess(
          processId,
          process -> {
            process.setStatus("FAILED");
            process.setFailureCause(shortMessage(e));
            process.setCompletedAt(OffsetDateTime.now());
          });
    }
  }

  private void processFile(
      UUID processId,
      String bucketName,
      String file,
      AtomicInteger processed,
      AtomicInteger failed) {
    updateProcess(processId, process -> process.setCurrentFile(file));
    try {
      ingestionService.ingestFromGcs(bucketName, file);
      int processedCount = processed.incrementAndGet();
      updateProcess(processId, process -> process.setProcessedFiles(processedCount));
    } catch (Exception e) {
      int failedCount = failed.incrementAndGet();
      log.warn("Failed to ingest project file: {}", file, e);
      updateProcess(
          processId,
          process -> {
            process.setFailedFiles(failedCount);
            process.setFailureCause(file + ": " + shortMessage(e));
          });
    }
  }

  private synchronized void updateProcess(
      UUID processId, java.util.function.Consumer<ProcessTrackingEntity> update) {
    processRepository
        .findById(processId)
        .ifPresent(
            process -> {
              update.accept(process);
              processRepository.save(process);
            });
  }

  private String shortMessage(Exception e) {
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      message = e.getClass().getSimpleName();
    }
    return message.length() <= 2000 ? message : message.substring(0, 2000);
  }
}
