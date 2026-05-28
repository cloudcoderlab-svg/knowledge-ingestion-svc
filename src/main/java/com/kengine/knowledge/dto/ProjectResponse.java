package com.kengine.knowledge.dto;

import com.kengine.knowledge.entity.ProjectStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
    UUID projectId,
    String projectName,
    Integer version,
    String title,
    String description,
    String definition,
    String sourceBucket,
    String gcsPrefix,
    ProjectStatus status,
    String metadata,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
