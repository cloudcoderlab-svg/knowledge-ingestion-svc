package com.kengine.ingestion.dto;

import lombok.Data;

@Data
public class BusinessComponent {
  private String componentName;
  private String componentType;
  private String capability;
  private String owner;
  private String lifecycle;
  private Double confidence;
}
