package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeComponent {
  private UUID componentId;
  private String artifactId;
  private String projectId;
  private UUID domainId;
  private UUID subdomainId;
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
  private List<Double> embedding;
  private Map<String, Object> metadata;
  private OffsetDateTime createdAt;
}
