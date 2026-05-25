package com.kengine.ingestion.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {

  private UUID subjectId;
  private String subjectName;
  private String title;
  private String description;
  private String sourceBucket;
  private String gcsFolderUrl;
  private String uploadUrl;
  private String definitionUrl;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
