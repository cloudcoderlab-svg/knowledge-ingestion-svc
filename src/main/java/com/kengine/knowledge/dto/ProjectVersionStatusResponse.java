package com.kengine.knowledge.dto;

import com.kengine.knowledge.entity.ProjectStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectVersionStatusResponse(
    UUID projectId,
    String projectName,
    Integer version,
    String title,
    ProjectStatus status,
    String sourceBucket,
    List<ProcessResponse> runningProcesses,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
