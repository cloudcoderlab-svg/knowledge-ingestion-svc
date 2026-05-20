package com.kengine.ingestion.dto;

import lombok.Data;

@Data
public class BusinessFlowStep {
  private Integer sequence;
  private String stepName;
  private String actor;
  private String action;
  private String input;
  private String output;
  private String nextStep;
}
