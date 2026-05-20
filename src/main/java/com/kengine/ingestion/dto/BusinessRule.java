package com.kengine.ingestion.dto;

import lombok.Data;

@Data
public class BusinessRule {
  private String ruleName;
  private String ruleType;
  private String condition;
  private String outcome;
  private String sourceBusinessComponentName;
  private String priority;
  private Double confidence;
}
