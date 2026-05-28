package com.kengine.knowledge.dto;

import java.time.OffsetDateTime;

public record SignedUrlResponse(
    String uploadUrl, String bucket, String objectName, OffsetDateTime expiresAt) {}
