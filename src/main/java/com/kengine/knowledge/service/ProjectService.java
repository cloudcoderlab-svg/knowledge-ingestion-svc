package com.kengine.knowledge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.kengine.knowledge.dto.*;
import com.kengine.knowledge.entity.*;
import com.kengine.knowledge.exception.NotFoundException;
import com.kengine.knowledge.ingestion.service.ai.EmbeddingService;
import com.kengine.knowledge.ingestion.util.EmbeddingUtils;
import com.kengine.knowledge.repository.ProcessTrackingRepository;
import com.kengine.knowledge.repository.ProjectRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {
  private final ProjectRepository projectRepository;
  private final ProcessTrackingRepository processRepository;
  private final ProjectPathService pathService;
  private final ObjectMapper objectMapper;
  private final Storage storage;
  private final EmbeddingService embeddingService;

  @Value("${gcp.storage.bucket-name}")
  private String defaultBucketName;

  @Transactional
  public ProjectResponse create(CreateProjectRequest request) {
    String normalizedName = pathService.slug(request.projectName());
    String sourceBucket = resolveSourceBucket(request.sourceBucket());
    String gcsPrefix = pathService.projectPrefix(normalizedName);

    int version;
    if (request.version() != null) {
      version = request.version();
      projectRepository
          .findByProjectNameAndVersion(normalizedName, version)
          .ifPresent(
              existing -> {
                throw new IllegalArgumentException("Project name and version already exist");
              });
    } else {
      Integer maxVersion = projectRepository.findMaxVersionByProjectName(normalizedName);
      version = (maxVersion == null ? 0 : maxVersion) + 1;
    }

    ProjectEntity project =
        ProjectEntity.builder()
            .projectName(normalizedName)
            .version(version)
            .title(request.title())
            .description(request.description())
            .definition(request.definition())
            .definitionEmbedding(definitionEmbedding(request.definition()))
            .sourceBucket(sourceBucket)
            .gcsPrefix(gcsPrefix)
            .status(ProjectStatus.DRAFT)
            .metadata(toJson(request.metadata()))
            .build();
    ProjectEntity savedProject = projectRepository.save(project);
    createProjectDefinition(sourceBucket, gcsPrefix, request.definition());
    return toResponse(savedProject);
  }

  private void suspendRunningProcesses(UUID projectId) {
    List<ProcessTrackingEntity> runningProcesses =
        processRepository.findByProjectIdAndStatus(projectId, "RUNNING");

    for (ProcessTrackingEntity process : runningProcesses) {
      process.setStatus("SUSPENDED");
      process.setFailureCause("Process suspended due to new project version launch");
      process.setCompletedAt(java.time.OffsetDateTime.now());
      processRepository.save(process);
    }
    if (!runningProcesses.isEmpty()) {
      log.info(
          "Suspended {} running process(es) for project: {}", runningProcesses.size(), projectId);
    }
  }

  @Transactional
  public void onIngestionSuccess(UUID projectId, String processStatus) {
    ProjectEntity project = find(projectId);

    // Mark project as ACTIVE for both COMPLETED and PARTIAL_SUCCESS
    // Multiple versions can coexist as ACTIVE
    project.setStatus(ProjectStatus.ACTIVE);
    projectRepository.save(project);
    log.info(
        "Project {} (v{}) marked as ACTIVE after {} ingestion",
        project.getProjectName(),
        project.getVersion(),
        processStatus);

    // Suspend any in-progress/running previous versions
    // This prevents multiple versions from ingesting simultaneously
    List<ProjectEntity> inProgressVersions =
        projectRepository.findAll().stream()
            .filter(
                p ->
                    p.getProjectName().equals(project.getProjectName())
                        && !p.getProjectId().equals(projectId)
                        && (p.getStatus() == ProjectStatus.DRAFT
                            || p.getStatus() == ProjectStatus.INGESTING))
            .toList();

    for (ProjectEntity inProgressProject : inProgressVersions) {
      inProgressProject.setStatus(ProjectStatus.SUSPENDED);
      projectRepository.save(inProgressProject);
      suspendRunningProcesses(inProgressProject.getProjectId());
    }

    if (!inProgressVersions.isEmpty()) {
      log.info(
          "Suspended {} in-progress version(s) of project: {}",
          inProgressVersions.size(),
          project.getProjectName());
    }
  }

  public ProjectResponse get(UUID projectId) {
    return toResponse(find(projectId));
  }

  public List<ProjectResponse> list() {
    return projectRepository.findAll().stream().map(this::toResponse).toList();
  }

  public ProjectStatusSummaryResponse getProjectStatus(String projectName) {
    String normalizedName = pathService.slug(projectName);
    List<ProjectEntity> allVersions =
        projectRepository.findByProjectNameOrderByVersionDesc(normalizedName);

    if (allVersions.isEmpty()) {
      throw new NotFoundException("Project not found: " + projectName);
    }

    List<UUID> projectIds = allVersions.stream().map(ProjectEntity::getProjectId).toList();
    List<ProcessTrackingEntity> allProcesses =
        processRepository.findByProjectIdInOrderByCreatedAtDesc(projectIds);

    List<ProjectVersionStatusResponse> versionStatuses =
        allVersions.stream()
            .map(
                project -> {
                  List<ProcessResponse> runningProcesses =
                      allProcesses.stream()
                          .filter(p -> p.getProjectId().equals(project.getProjectId()))
                          .filter(p -> "RUNNING".equals(p.getStatus()))
                          .map(this::toProcessResponse)
                          .toList();

                  return new ProjectVersionStatusResponse(
                      project.getProjectId(),
                      project.getProjectName(),
                      project.getVersion(),
                      project.getTitle(),
                      project.getStatus(),
                      project.getSourceBucket(),
                      runningProcesses,
                      project.getCreatedAt(),
                      project.getUpdatedAt());
                })
            .toList();

    return new ProjectStatusSummaryResponse(normalizedName, allVersions.size(), versionStatuses);
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

  ProjectEntity find(UUID projectId) {
    return projectRepository
        .findById(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
  }

  public ProjectEntity findByNameAndVersion(String projectName, Integer version) {
    String normalizedName = pathService.slug(projectName);
    return projectRepository
        .findByProjectNameAndVersion(normalizedName, version)
        .orElseThrow(
            () ->
                new NotFoundException("Project not found: " + projectName + " version " + version));
  }

  ProjectResponse toResponse(ProjectEntity project) {
    return new ProjectResponse(
        project.getProjectId(),
        project.getProjectName(),
        project.getVersion(),
        project.getTitle(),
        project.getDescription(),
        project.getDefinition(),
        project.getSourceBucket(),
        project.getGcsPrefix(),
        project.getStatus(),
        project.getMetadata(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  private String resolveSourceBucket(String sourceBucket) {
    return sourceBucket == null || sourceBucket.isBlank() ? defaultBucketName : sourceBucket;
  }

  private String toJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      log.warn("Could not serialize project metadata", e);
      return null;
    }
  }

  private void createProjectDefinition(String bucket, String prefix, String definition) {
    if (definition == null || definition.isBlank()) {
      return;
    }
    BlobInfo definitionFile =
        BlobInfo.newBuilder(bucket, prefix + "definition.md")
            .setContentType("text/markdown")
            .setMetadata(
                java.util.Map.of(
                    "managed-by", "knowledge_engine_svc",
                    "purpose", "project-definition"))
            .build();
    storage.create(definitionFile, definition.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private String definitionEmbedding(String definition) {
    if (definition == null || definition.isBlank()) {
      return null;
    }
    try {
      return EmbeddingUtils.embeddingToString(embeddingService.embedding(definition));
    } catch (Exception e) {
      log.warn("Could not generate project definition embedding", e);
      return null;
    }
  }
}
