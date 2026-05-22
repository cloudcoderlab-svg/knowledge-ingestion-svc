package com.kengine.ingestion.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiagramContent {
  private String description;
  private byte[] imageData;
  private int pageNumber;
  private Double confidence;
}
