package com.kengine.ingestion.dto;

import lombok.Data;

@Data
public class TechnicalComponent {
  private String componentName;
  private String componentType;
  private String responsibility;
  private String technology;
  private String lifecycle;
  private Double confidence;
}
