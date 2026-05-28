package com.kengine.knowledge.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProcessResponse(
    UUID processId,
    UUID projectId,
    String processType,
    String status,
    Integer totalFiles,
    Integer processedFiles,
    Integer failedFiles,
    String currentFile,
    String failureCause,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
