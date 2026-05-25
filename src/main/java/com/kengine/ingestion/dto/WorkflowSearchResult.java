package com.kengine.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSearchResult {
  private UUID workflowId;
  private UUID artifactId;
  private UUID subjectId;
  private UUID domainId;
  private String workflowName;
  private String triggerText;
  private String outcomeText;
  private String owner;
  private Double confidence;
  private Double similarityScore;
}
