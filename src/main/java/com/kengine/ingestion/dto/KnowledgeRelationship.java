package com.kengine.ingestion.dto;

import lombok.Data;

@Data
public class KnowledgeRelationship {
  private String sourceName;
  private String sourceType;
  private String targetName;
  private String targetType;
  private String relationshipType;
  private String context;
  private Double confidence;
}
