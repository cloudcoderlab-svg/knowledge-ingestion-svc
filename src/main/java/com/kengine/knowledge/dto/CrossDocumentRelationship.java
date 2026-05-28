package com.kengine.knowledge.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossDocumentRelationship {
  private String sourceDocument;
  private String targetDocument;
  private String sourceName;
  private String sourceType;
  private String targetName;
  private String targetType;
  private String relationshipType;
  private String context;
  private Double confidence;
}
