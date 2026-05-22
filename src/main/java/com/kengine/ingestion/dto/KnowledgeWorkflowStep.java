package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeWorkflowStep {
  private UUID stepId;
  private UUID workflowId;
  private Integer sequenceNumber;
  private String stepName;
  private String actor;
  private String actionText;
  private String inputData;
  private String outputData;
  private String nextStep;
  private List<Double> embedding;
  private OffsetDateTime createdAt;
}
