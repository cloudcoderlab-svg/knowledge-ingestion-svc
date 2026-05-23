package com.kengine.ingestion.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRuleDto {
  private UUID ruleId;
  private String artifactId;
  private String ruleName;
  private String ruleType;
  private String conditionText;
  private String outcomeText;
  private String priority;
  private Double confidence;
}
