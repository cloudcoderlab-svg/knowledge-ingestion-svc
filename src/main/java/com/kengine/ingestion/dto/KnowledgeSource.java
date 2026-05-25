package com.kengine.ingestion.dto;

import java.util.UUID;

public record KnowledgeSource(
    UUID artifactId,
    UUID subjectId,
    String domain,
    String subdomain,
    String sourceBucket,
    String sourceObject,
    Long sourceGeneration,
    String sourceChecksum,
    String contentHash,
    String artifactType,
    String fileType,
    String title,
    Integer version,
    Boolean current) {}
