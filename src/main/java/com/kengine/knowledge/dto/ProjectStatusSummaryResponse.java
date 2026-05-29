package com.kengine.knowledge.dto;

import java.util.List;

public record ProjectStatusSummaryResponse(
    String projectName, Integer totalVersions, List<ProjectVersionStatusResponse> versions) {}
