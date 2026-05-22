package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeDataField {
  private UUID fieldId;
  private UUID dataModelId;
  private String fieldName;
  private String fieldType;
  private Boolean isRequired;
  private String description;
  private Map<String, Object> constraints;
  private OffsetDateTime createdAt;
}
