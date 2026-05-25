package com.kengine.ingestion.dto;

import java.util.UUID;

public record SourceChunk(
    UUID chunkId,
    UUID documentId,
    UUID artifactId,
    UUID subjectId,
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
