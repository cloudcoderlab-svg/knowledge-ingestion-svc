package com.kengine.ingestion.dto;

import java.util.UUID;

public record SourceDocumentMetadata(
    UUID subjectId,
    String bucketName,
    String objectName,
    Long generation,
    String checksum,
    String contentHash,
    String artifactType,
    String fileType,
    String title) {}
