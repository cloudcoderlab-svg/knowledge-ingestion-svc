package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeAPI {
  private UUID apiId;
  private UUID componentId;
  private UUID artifactId;
  private UUID subjectId;
  private String apiName;
  private String apiType;
  private String httpMethod;
  private String endpointPath;
  private String description;
  private Map<String, Object> requestSchema;
  private Map<String, Object> responseSchema;
  private String authentication;
  private List<Double> embedding;
  private OffsetDateTime createdAt;
}
