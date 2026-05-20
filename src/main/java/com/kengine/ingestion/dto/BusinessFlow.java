package com.kengine.ingestion.dto;

import java.util.List;
import lombok.Data;

@Data
public class BusinessFlow {
  private String flowName;
  private String trigger;
  private String outcome;
  private String owner;
  private List<BusinessFlowStep> steps;
  private Double confidence;
}
