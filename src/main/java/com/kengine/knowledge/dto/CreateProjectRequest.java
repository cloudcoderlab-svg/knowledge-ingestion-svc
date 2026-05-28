package com.kengine.knowledge.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateProjectRequest(
    @NotBlank String projectName,
    @Positive Integer version,
    String title,
    String description,
    String definition,
    String sourceBucket,
    JsonNode metadata) {}
