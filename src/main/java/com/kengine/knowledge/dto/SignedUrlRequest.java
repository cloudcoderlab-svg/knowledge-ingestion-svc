package com.kengine.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record SignedUrlRequest(
    @NotBlank String fileName, String contentType, Integer expirationMinutes) {}
