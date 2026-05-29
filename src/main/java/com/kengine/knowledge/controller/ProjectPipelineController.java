package com.kengine.knowledge.controller;

import com.kengine.knowledge.dto.ProcessResponse;
import com.kengine.knowledge.dto.ProjectPipelineRequest;
import com.kengine.knowledge.service.ProjectPipelineService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectPipelineController {
  private final ProjectPipelineService projectPipelineService;

  @PostMapping("/{projectId}/processes/pipeline")
  public ProcessResponse startProjectPipeline(@PathVariable UUID projectId) {
    return projectPipelineService.startProjectPipeline(projectId);
  }

  @PostMapping("/processes/pipeline")
  public List<ProcessResponse> startProjectPipelines(
      @Valid @RequestBody ProjectPipelineRequest request) {
    return projectPipelineService.startProjectPipelines(request.projectIds());
  }
}
