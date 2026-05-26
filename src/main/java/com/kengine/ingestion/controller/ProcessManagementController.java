package com.kengine.ingestion.controller;

import com.kengine.ingestion.dto.ProcessResponse;
import com.kengine.ingestion.dto.StartProcessRequest;
import com.kengine.ingestion.entity.ProcessTrackingEntity;
import com.kengine.ingestion.model.ProcessStatus;
import com.kengine.ingestion.service.ProcessTrackingService;
import com.kengine.ingestion.service.VirtualThreadProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing document ingestion processes.
 *
 * <p>Provides endpoints to start, monitor, and query processing jobs that ingest documents from GCS
 * and extract knowledge entities.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Process Management",
    description =
        "APIs for managing document ingestion processes, including starting processing jobs, "
            + "monitoring progress, and retrieving detailed status information")
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
  @Operation(
      summary = "Start document ingestion process",
      description =
          "Initiates parallel processing of documents from a subject's GCS folder. "
              + "Processes all files or optionally only specified files. Uses virtual threads for parallel execution.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Process successfully created and started",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProcessResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request (subject not found or invalid file list)",
            content = @Content),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during process initiation",
            content = @Content)
      })
  @PostMapping("/subjects/{subjectId}/process")
  public ResponseEntity<ProcessResponse> startProcessing(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId,
      @Parameter(description = "Optional list of specific files to process")
          @Valid
          @RequestBody(required = false)
          StartProcessRequest request) {
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
  @Operation(
      summary = "Get process status",
      description =
          "Retrieves detailed status information for a processing job, including progress, "
              + "file counts, and completion status.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Process status retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProcessResponse.class))),
        @ApiResponse(responseCode = "404", description = "Process not found", content = @Content)
      })
  @GetMapping("/processes/{processId}")
  public ResponseEntity<ProcessResponse> getProcessStatus(
      @Parameter(description = "Unique identifier of the process", required = true) @PathVariable
          UUID processId) {
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
  @Operation(
      summary = "List processes for a subject",
      description =
          "Retrieves all processing jobs for a specific subject. Can optionally filter by status "
              + "(INIT, IN_PROGRESS, SUCCESS, FAILED).")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Processes retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Subject not found", content = @Content)
      })
  @GetMapping("/subjects/{subjectId}/processes")
  public ResponseEntity<List<ProcessResponse>> listSubjectProcesses(
      @Parameter(description = "Unique identifier of the subject", required = true) @PathVariable
          UUID subjectId,
      @Parameter(
              description = "Optional status filter (INIT, IN_PROGRESS, SUCCESS, FAILED)",
              required = false)
          @RequestParam(required = false)
          ProcessStatus status) {
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
  @Operation(
      summary = "List processes by status",
      description =
          "Retrieves all processing jobs across all subjects, filtered by status. "
              + "Useful for monitoring system-wide processing activity. "
              + "Status parameter is required - use the subject-specific endpoint to get all processes for a subject.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Processes retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Status parameter is required",
            content = @Content)
      })
  @GetMapping("/processes")
  public ResponseEntity<List<ProcessResponse>> listProcessesByStatus(
      @Parameter(
              description = "Status filter (INIT, IN_PROGRESS, SUCCESS, FAILED)",
              required = true)
          @RequestParam
          ProcessStatus status) {
    log.info("Listing processes with status: {}", status);
    List<ProcessResponse> response = processTrackingService.listProcessesByStatus(status);
    return ResponseEntity.ok(response);
  }

  /**
   * Gets document processing details for a process.
   *
   * @param processId Process UUID
   * @return List of document tracking details
   */
  @Operation(
      summary = "Get document-level processing status",
      description =
          "Retrieves detailed status for each individual document in a processing job. "
              + "Shows which documents are pending, in progress, successful, or failed, along with error details.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Document status retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Process not found", content = @Content)
      })
  @GetMapping("/processes/{processId}/documents")
  public ResponseEntity<?> getDocumentProcessingStatus(
      @Parameter(description = "Unique identifier of the process", required = true) @PathVariable
          UUID processId) {
    log.info("Getting document processing status for process: {}", processId);
    return ResponseEntity.ok(
        Map.of(
            "processId",
            processId,
            "documents",
            virtualThreadProcessingService.getDocumentStatus(processId)));
  }

  /**
   * Resumes a failed or incomplete process.
   *
   * @param processId Process UUID
   * @return Process response with updated status
   */
  @Operation(
      summary = "Resume a process",
      description =
          "Resumes a failed or incomplete ingestion process. "
              + "Can only resume processes with status FAILED. "
              + "The process will restart processing from the beginning.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Process successfully resumed",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProcessResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot resume process (already completed or in progress)",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Process not found", content = @Content)
      })
  @PostMapping("/processes/{processId}/resume")
  public ResponseEntity<ProcessResponse> resumeProcess(
      @Parameter(description = "Unique identifier of the process to resume", required = true)
          @PathVariable
          UUID processId) {
    log.info("Resuming process: {}", processId);

    // Get current process status
    ProcessResponse currentProcess = processTrackingService.getProcess(processId);

    // Validate process can be resumed
    if (currentProcess.getStatus() == ProcessStatus.IN_PROGRESS) {
      throw new IllegalStateException(
          "Process is already in progress. Current status: " + currentProcess.getStatus());
    }

    if (currentProcess.getStatus() == ProcessStatus.SUCCESS) {
      throw new IllegalStateException(
          "Process already completed successfully. Cannot resume completed processes.");
    }

    // Only FAILED or INIT processes can be resumed
    if (currentProcess.getStatus() != ProcessStatus.FAILED
        && currentProcess.getStatus() != ProcessStatus.INIT) {
      throw new IllegalStateException(
          "Can only resume FAILED or INIT processes. Current status: "
              + currentProcess.getStatus());
    }

    // Start processing asynchronously with virtual threads
    virtualThreadProcessingService.startProcessing(processId);

    log.info("Process {} resumed successfully", processId);

    // Return updated process details
    ProcessResponse response = processTrackingService.getProcess(processId);
    return ResponseEntity.ok(response);
  }
}
