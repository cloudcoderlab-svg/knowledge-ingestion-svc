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
  private final ProjectPathService pathService;
  private final ObjectMapper objectMapper;
  private final Storage storage;
  private final EmbeddingService embeddingService;

  @Value("${gcp.storage.bucket-name}")
  private String defaultBucketName;

  @Transactional
  public ProjectResponse create(CreateProjectRequest request) {
    int version = request.version() == null ? 1 : request.version();
    projectRepository
        .findByProjectNameAndVersion(request.projectName(), version)
        .ifPresent(
            existing -> {
              throw new IllegalArgumentException("Project name and version already exist");
            });

    String sourceBucket = resolveSourceBucket(request.sourceBucket());
    String gcsPrefix = pathService.projectPrefix(request.projectName());
    ProjectEntity project =
        ProjectEntity.builder()
            .projectName(request.projectName())
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

  public ProjectResponse get(UUID projectId) {
    return toResponse(find(projectId));
  }

  public List<ProjectResponse> list() {
    return projectRepository.findAll().stream().map(this::toResponse).toList();
  }

  ProjectEntity find(UUID projectId) {
    return projectRepository
        .findById(projectId)
        .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
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
