package com.kengine.knowledge.controller;

import com.kengine.knowledge.dto.*;
import com.kengine.knowledge.service.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
  private final ProjectService projectService;
  private final GcsProjectFileService fileService;

  @PostMapping
  public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
  }

  @GetMapping("/{projectId}")
  public ProjectResponse get(@PathVariable UUID projectId) {
    return projectService.get(projectId);
  }

  @GetMapping
  public List<ProjectResponse> list() {
    return projectService.list();
  }

  @PostMapping("/{projectId}/upload-url")
  public SignedUrlResponse uploadUrl(
      @PathVariable UUID projectId, @Valid @RequestBody SignedUrlRequest request) {
    return fileService.uploadUrl(projectId, request);
  }

  @GetMapping("/{projectId}/files")
  public List<String> files(@PathVariable UUID projectId) {
    return fileService.listFiles(projectId);
  }
}
