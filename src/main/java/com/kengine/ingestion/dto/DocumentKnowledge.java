package com.kengine.ingestion.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentKnowledge {
  private String overallArchitecture;
  private String systemSummary;
  private List<String> keyPatterns;
  private List<String> technologies;
  private String domain;
  private String subdomain;
  private List<String> identifiedComponents;
  private List<String> identifiedAPIs;
  private List<String> identifiedWorkflows;
  private Map<String, Object> additionalMetadata;

  // Platform detection for routing to specialized prompts
  private String detectedPlatform; // e.g., "TIBCO_MDM", "PEGA_BPM", "CAMUNDA_BPMN", etc.
}
