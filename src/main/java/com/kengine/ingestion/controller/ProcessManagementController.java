package com.kengine.ingestion.controller;

import com.kengine.ingestion.dto.ProcessResponse;
import com.kengine.ingestion.dto.StartProcessRequest;
import com.kengine.ingestion.entity.ProcessTrackingEntity;
import com.kengine.ingestion.model.ProcessStatus;
import com.kengine.ingestion.service.ProcessTrackingService;
import com.kengine.ingestion.service.VirtualThreadProcessingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ProcessManagementController {

  private final ProcessTrackingService processTrackingService;
  private final VirtualThreadProcessingService virtualThreadProcessingService;

  /**
   * Starts processing files for a subject.
   *
   * @param subjectId Subject UUID
   * @param request Start process request (optional file list)
   * @return Process response with process ID and initial status
   */
  @PostMapping("/subjects/{subjectId}/process")
  public ResponseEntity<ProcessResponse> startProcessing(
      @PathVariable UUID subjectId,
      @Valid @RequestBody(required = false) StartProcessRequest request) {
    log.info("Starting processing for subject: {}", subjectId);

    List<String> files = request != null ? request.getFiles() : null;

    // Create process record
    ProcessTrackingEntity process = processTrackingService.createProcess(subjectId, files);

    // Start processing asynchronously with virtual threads
    virtualThreadProcessingService.startProcessing(process.getProcessId());

    // Return process details
    ProcessResponse response = processTrackingService.getProcess(process.getProcessId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Gets the status and details of a process.
   *
   * @param processId Process UUID
   * @return Process response with current status and progress
   */
  @GetMapping("/processes/{processId}")
  public ResponseEntity<ProcessResponse> getProcessStatus(@PathVariable UUID processId) {
    log.info("Getting process status: {}", processId);
    ProcessResponse response = processTrackingService.getProcess(processId);
    return ResponseEntity.ok(response);
  }

  /**
   * Lists all processes for a subject, optionally filtered by status.
   *
   * @param subjectId Subject UUID
   * @param status Optional process status filter (e.g., SUCCESS, FAILED, IN_PROGRESS)
   * @return List of process responses
   */
  @GetMapping("/subjects/{subjectId}/processes")
  public ResponseEntity<List<ProcessResponse>> listSubjectProcesses(
      @PathVariable UUID subjectId, @RequestParam(required = false) ProcessStatus status) {
    if (status != null) {
      log.info("Listing processes for subject: {} with status: {}", subjectId, status);
      List<ProcessResponse> response =
          processTrackingService.listProcessesBySubjectAndStatus(subjectId, status);
      return ResponseEntity.ok(response);
    } else {
      log.info("Listing all processes for subject: {}", subjectId);
      List<ProcessResponse> response = processTrackingService.listProcessesBySubject(subjectId);
      return ResponseEntity.ok(response);
    }
  }

  /**
   * Lists processes by status.
   *
   * @param status Process status
   * @return List of process responses
   */
  @GetMapping("/processes")
  public ResponseEntity<List<ProcessResponse>> listProcessesByStatus(
      @RequestParam(required = false) ProcessStatus status) {
    if (status != null) {
      log.info("Listing processes with status: {}", status);
      List<ProcessResponse> response = processTrackingService.listProcessesByStatus(status);
      return ResponseEntity.ok(response);
    } else {
      log.info("Listing all processes");
      // Could implement listAll if needed
      return ResponseEntity.ok(List.of());
    }
  }
}
