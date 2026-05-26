package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeBusinessRule {
  private UUID ruleId;
  private UUID artifactId;
  private UUID componentId;
  private UUID subjectId;
  private UUID domainId;
  private String ruleName;
  private String ruleType;
  private String conditionText;
  private String outcomeText;
  private String priority;
  private Double confidence;
  private List<Double> embedding;
  private String technicalImplementation;
  private String validationCriteria;
  private OffsetDateTime createdAt;
}
