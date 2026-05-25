package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.ProcessResponse;
import com.kengine.ingestion.entity.ProcessTrackingEntity;
import com.kengine.ingestion.model.ProcessStatus;
import com.kengine.ingestion.repository.ProcessTrackingRepository;
import com.kengine.ingestion.repository.SubjectRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessTrackingService {

  private final ProcessTrackingRepository processTrackingRepository;
  private final SubjectRepository subjectRepository;
  private final GcsFolderManagementService gcsFolderManagementService;

  /**
   * Creates a new process record for a subject.
   *
   * @param subjectId Subject UUID
   * @param files List of files to process (null means all files in subject folder)
   * @return Created process entity
   */
  @Transactional
  public ProcessTrackingEntity createProcess(UUID subjectId, List<String> files) {
    // Verify subject exists
    if (!subjectRepository.existsById(subjectId)) {
      throw new IllegalArgumentException("Subject not found: " + subjectId);
    }

    // If no files specified, get all files from subject folder
    List<String> filesToProcess = files;
    if (filesToProcess == null || filesToProcess.isEmpty()) {
      String subjectName =
          subjectRepository
              .findById(subjectId)
              .orElseThrow(() -> new IllegalArgumentException("Subject not found"))
              .getSubjectName();
      filesToProcess = gcsFolderManagementService.listSubjectFiles(subjectName);
    }

    // Create process entity
    ProcessTrackingEntity process =
        ProcessTrackingEntity.builder()
            .subjectId(subjectId)
            .processType("DOCUMENT_INGESTION")
            .status(ProcessStatus.INIT)
            .totalFiles(filesToProcess.size())
            .processedFiles(0)
            .failedFiles(0)
            .fileList(filesToProcess)
            .build();

    process = processTrackingRepository.save(process);

    log.info(
        "Created process {} for subject {} with {} files",
        process.getProcessId(),
        subjectId,
        filesToProcess.size());

    return process;
  }

  /**
   * Updates the status of a process.
   *
   * @param processId Process UUID
   * @param status New status
   * @param failureCause Optional failure cause (for FAILED status)
   */
  @Transactional
  public void updateStatus(UUID processId, ProcessStatus status, String failureCause) {
    ProcessTrackingEntity process =
        processTrackingRepository
            .findById(processId)
            .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));

    process.setStatus(status);

    if (status == ProcessStatus.IN_PROGRESS && process.getStartedAt() == null) {
      process.setStartedAt(OffsetDateTime.now());
    }

    if (status == ProcessStatus.SUCCESS || status == ProcessStatus.FAILED) {
      process.setCompletedAt(OffsetDateTime.now());
    }

    if (failureCause != null) {
      process.setFailureCause(failureCause);
    }

    processTrackingRepository.save(process);

    log.info("Updated process {} status to {}", processId, status);
  }

  /**
   * Updates the progress of a process.
   *
   * @param processId Process UUID
   * @param processedFiles Number of successfully processed files
   * @param failedFiles Number of failed files
   * @param currentFile Current file being processed
   */
  @Transactional
  public void updateProgress(
      UUID processId, int processedFiles, int failedFiles, String currentFile) {
    ProcessTrackingEntity process =
        processTrackingRepository
            .findById(processId)
            .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));

    process.setProcessedFiles(processedFiles);
    process.setFailedFiles(failedFiles);
    process.setCurrentFile(currentFile);

    processTrackingRepository.save(process);

    log.debug(
        "Updated process {} progress: {}/{} processed, {} failed",
        processId,
        processedFiles,
        process.getTotalFiles(),
        failedFiles);
  }

  /**
   * Marks a file as processed (success or failure).
   *
   * @param processId Process UUID
   * @param file File path
   * @param success Whether the file was processed successfully
   * @param error Optional error message
   */
  @Transactional
  public void markFileProcessed(UUID processId, String file, boolean success, String error) {
    ProcessTrackingEntity process =
        processTrackingRepository
            .findById(processId)
            .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));

    int processed = process.getProcessedFiles() != null ? process.getProcessedFiles() : 0;
    int failed = process.getFailedFiles() != null ? process.getFailedFiles() : 0;

    if (success) {
      processed++;
    } else {
      failed++;

      // Track error details
      Map<String, Object> errorDetails = process.getErrorDetails();
      if (errorDetails == null) {
        errorDetails = new HashMap<>();
      }
      errorDetails.put(file, error);
      process.setErrorDetails(errorDetails);
    }

    process.setProcessedFiles(processed);
    process.setFailedFiles(failed);

    processTrackingRepository.save(process);

    log.info(
        "Marked file {} as {} for process {}", file, success ? "success" : "failed", processId);
  }

  /**
   * Gets a process by ID.
   *
   * @param processId Process UUID
   * @return Process response
   */
  public ProcessResponse getProcess(UUID processId) {
    ProcessTrackingEntity process =
        processTrackingRepository
            .findById(processId)
            .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));

    return toProcessResponse(process);
  }

  /**
   * Gets the process entity by ID (for internal use).
   *
   * @param processId Process UUID
   * @return Process entity
   */
  public ProcessTrackingEntity getProcessEntity(UUID processId) {
    return processTrackingRepository
        .findById(processId)
        .orElseThrow(() -> new IllegalArgumentException("Process not found: " + processId));
  }

  /**
   * Lists all processes for a subject.
   *
   * @param subjectId Subject UUID
   * @return List of process responses
   */
  public List<ProcessResponse> listProcessesBySubject(UUID subjectId) {
    return processTrackingRepository.findBySubjectIdOrderByCreatedAtDesc(subjectId).stream()
        .map(this::toProcessResponse)
        .collect(Collectors.toList());
  }

  /**
   * Lists processes by status.
   *
   * @param status Process status
   * @return List of process responses
   */
  public List<ProcessResponse> listProcessesByStatus(ProcessStatus status) {
    return processTrackingRepository.findByStatusOrderByCreatedAtDesc(status).stream()
        .map(this::toProcessResponse)
        .collect(Collectors.toList());
  }

  private ProcessResponse toProcessResponse(ProcessTrackingEntity process) {
    return ProcessResponse.builder()
        .processId(process.getProcessId())
        .subjectId(process.getSubjectId())
        .processType(process.getProcessType())
        .status(process.getStatus())
        .totalFiles(process.getTotalFiles())
        .processedFiles(process.getProcessedFiles())
        .failedFiles(process.getFailedFiles())
        .currentFile(process.getCurrentFile())
        .failureCause(process.getFailureCause())
        .errorDetails(process.getErrorDetails())
        .fileList(process.getFileList())
        .startedAt(process.getStartedAt())
        .completedAt(process.getCompletedAt())
        .createdAt(process.getCreatedAt())
        .updatedAt(process.getUpdatedAt())
        .build();
  }
}
