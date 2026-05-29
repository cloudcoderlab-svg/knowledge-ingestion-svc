package com.kengine.knowledge.controller;

import com.kengine.knowledge.dto.ProcessResponse;
import com.kengine.knowledge.dto.ProcessingSummaryResponse;
import com.kengine.knowledge.entity.ProcessTrackingEntity;
import com.kengine.knowledge.exception.NotFoundException;
import com.kengine.knowledge.repository.ProcessTrackingRepository;
import com.kengine.knowledge.service.ProcessingService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProcessingController {
  private final ProcessingService processingService;
  private final ProcessTrackingRepository processRepository;

  // Endpoints by project ID
  @PostMapping("/api/v1/projects/{projectId}/processes/ingestion")
  public ProcessResponse startIngestion(@PathVariable UUID projectId) {
    return processingService.startIngestion(projectId);
  }

  @PostMapping("/api/v1/projects/{projectId}/processes/consolidation")
  public ProcessResponse startConsolidation(@PathVariable UUID projectId) {
    return processingService.startConsolidation(projectId);
  }

  @PostMapping("/api/v1/projects/{projectId}/processes/planning")
  public ProcessResponse startPlanning(@PathVariable UUID projectId) {
    return processingService.startPlanning(projectId);
  }

  @GetMapping("/api/v1/projects/{projectId}/processes")
  public List<ProcessResponse> list(@PathVariable UUID projectId) {
    return processingService.list(projectId);
  }

  @GetMapping("/api/v1/projects/{projectId}/processes/summary")
  public ProcessingSummaryResponse getProcessingSummary(@PathVariable UUID projectId) {
    return processingService.getProcessingSummary(projectId);
  }

  // Endpoints by project name and version
  @GetMapping("/api/v1/processing/{projectName}/versions/{version}/processes")
  public List<ProcessResponse> listByVersion(
      @PathVariable String projectName, @PathVariable Integer version) {
    return processingService.listByProjectVersion(projectName, version);
  }

  @GetMapping("/api/v1/processing/{projectName}/versions/{version}/processes/summary")
  public ProcessingSummaryResponse getSummaryByVersion(
      @PathVariable String projectName, @PathVariable Integer version) {
    return processingService.getProcessingSummaryByVersion(projectName, version);
  }

  // Direct process access by ID
  @GetMapping("/api/v1/processes/{processId}")
  public ProcessResponse getProcessById(@PathVariable UUID processId) {
    ProcessTrackingEntity process =
        processRepository
            .findById(processId)
            .orElseThrow(() -> new NotFoundException("Process not found: " + processId));
    return toProcessResponse(process);
  }

  private ProcessResponse toProcessResponse(ProcessTrackingEntity process) {
    return new ProcessResponse(
        process.getProcessId(),
        process.getProjectId(),
        process.getProcessType(),
        process.getStatus(),
        process.getTotalFiles(),
        process.getProcessedFiles(),
        process.getFailedFiles(),
        process.getCurrentFile(),
        process.getFailureCause(),
        process.getTotalTokensProcessed(),
        process.getTotalBytesProcessed(),
        process.getStartedAt(),
        process.getCompletedAt(),
        process.getCreatedAt(),
        process.getUpdatedAt());
  }
}
