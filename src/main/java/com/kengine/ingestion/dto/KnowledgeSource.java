package com.kengine.ingestion.dto;

public record KnowledgeSource(
    String artifactId,
    String projectId,
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
    String version,
    Boolean current) {}
