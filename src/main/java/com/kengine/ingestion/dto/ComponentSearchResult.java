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
public class ComponentSearchResult {
  private UUID componentId;
  private UUID artifactId;
  private UUID subjectId;
  private UUID domainId;
  private String componentName;
  private String componentType;
  private String category;
  private String description;
  private String responsibility;
  private String technology;
  private String capability;
  private String owner;
  private String lifecycle;
  private Double confidence;
  private Double similarityScore;
}
