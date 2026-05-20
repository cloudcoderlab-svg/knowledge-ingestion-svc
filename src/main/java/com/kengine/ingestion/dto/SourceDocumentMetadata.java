package com.kengine.ingestion.dto;

public record SourceDocumentMetadata(
    String projectId,
    String bucketName,
    String objectName,
    Long generation,
    String checksum,
    String contentHash,
    String artifactType,
    String fileType,
    String title) {}
