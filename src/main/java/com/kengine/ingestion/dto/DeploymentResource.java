package com.kengine.ingestion.dto;

import java.util.List;
import lombok.Data;

@Data
public class DeploymentResource {
  private String resourceName;
  private String resourceType;
  private String provider;
  private String hostingModel;
  private String environment;
  private String region;
  private String criticality;
  private String lifecycle;
  private List<ResourceConfig> configs;
  private Double confidence;
}
