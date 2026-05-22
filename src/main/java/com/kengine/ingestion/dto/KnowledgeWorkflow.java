package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeWorkflow {
  private UUID workflowId;
  private String artifactId;
  private String projectId;
  private UUID domainId;
  private String workflowName;
  private String triggerText;
  private String outcomeText;
  private String owner;
  private Double confidence;
  private List<Double> embedding;
  private List<KnowledgeWorkflowStep> steps;
  private OffsetDateTime createdAt;
}
