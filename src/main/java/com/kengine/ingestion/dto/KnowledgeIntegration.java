package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeIntegration {
  private UUID integrationId;
  private UUID artifactId;
  private UUID componentId;
  private UUID subjectId;
  private String integrationName;
  private String integrationType;
  private String sourceSystem;
  private String targetSystem;
  private String protocol;
  private String description;
  private List<Double> embedding;
  private OffsetDateTime createdAt;
}
