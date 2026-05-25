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
public class SemanticChunkSearchResult {
  private UUID chunkId;
  private UUID artifactId;
  private UUID subjectId;
  private String sourceObject;
  private String domain;
  private Integer chunkIndex;
  private String content;
  private String contentHash;
  private Double similarityScore;
}
