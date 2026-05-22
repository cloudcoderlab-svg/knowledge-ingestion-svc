package com.kengine.ingestion.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchRequest {
  private String query;
  private String projectId;
  private List<String> entityTypes;
  private Integer limit;
  private Double minConfidence;
}
