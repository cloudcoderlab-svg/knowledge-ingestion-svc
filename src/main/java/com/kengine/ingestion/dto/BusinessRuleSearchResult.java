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
public class BusinessRuleSearchResult {
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
  private Double similarityScore;
}
