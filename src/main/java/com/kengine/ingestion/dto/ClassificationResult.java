package com.kengine.ingestion.dto;

import java.util.List;
import lombok.Data;

@Data
public class ClassificationResult {

  private String domain;
  private String subdomain;
  private String businessCapability;
  private String technicalCapability;
  private List<String> entities;
  private List<String> integrationTypes;
  private Double confidence;
  private String summary;
}
