package com.kengine.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
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
