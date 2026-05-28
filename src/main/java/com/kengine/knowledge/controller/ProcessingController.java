package com.kengine.knowledge.controller;

import com.kengine.knowledge.dto.ProcessResponse;
import com.kengine.knowledge.service.ProcessingService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/processes")
@RequiredArgsConstructor
public class ProcessingController {
  private final ProcessingService processingService;

  @PostMapping("/ingestion")
  public ProcessResponse startIngestion(@PathVariable UUID projectId) {
    return processingService.startIngestion(projectId);
  }

  @PostMapping("/consolidation")
  public ProcessResponse startConsolidation(@PathVariable UUID projectId) {
    return processingService.startConsolidation(projectId);
  }

  @PostMapping("/planning")
  public ProcessResponse startPlanning(@PathVariable UUID projectId) {
    return processingService.startPlanning(projectId);
  }

  @GetMapping
  public List<ProcessResponse> list(@PathVariable UUID projectId) {
    return processingService.list(projectId);
  }
}
