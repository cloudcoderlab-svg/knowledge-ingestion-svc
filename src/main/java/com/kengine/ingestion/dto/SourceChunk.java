package com.kengine.ingestion.dto;

public record SourceChunk(
    String chunkId,
    String documentId,
    String artifactId,
    String projectId,
    String sourceBucket,
    String sourceObject,
    Long sourceGeneration,
    String sourceChecksum,
    String documentContentHash,
    Long chunkIndex,
    Long totalChunks,
    Long charStart,
    Long charEnd,
    String chunkContentHash,
    String domain,
    String subdomain,
    String content) {}
