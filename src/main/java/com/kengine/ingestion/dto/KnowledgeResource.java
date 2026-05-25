package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeResource {
  private UUID resourceId;
  private UUID artifactId;
  private UUID componentId;
  private UUID subjectId;
  private String resourceName;
  private String resourceType;
  private String provider;
  private String hostingModel;
  private String environment;
  private String region;
  private String criticality;
  private String lifecycle;
  private Map<String, Object> configs;
  private Double confidence;
  private List<Double> embedding;
  private OffsetDateTime createdAt;
}
