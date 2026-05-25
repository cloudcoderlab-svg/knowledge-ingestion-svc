package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeDataModel {
  private UUID dataModelId;
  private UUID artifactId;
  private UUID subjectId;
  private UUID domainId;
  private String modelName;
  private String modelType;
  private String description;
  private Map<String, Object> schemaDefinition;
  private List<Double> embedding;
  private List<KnowledgeDataField> fields;
  private OffsetDateTime createdAt;
}
