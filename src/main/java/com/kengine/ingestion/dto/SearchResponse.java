package com.kengine.ingestion.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResponse {
  private String query;
  private Integer totalResults;
  private List<SearchResult> results;

  @Data
  @Builder
  public static class SearchResult {
    private String entityType;
    private String entityId;
    private String entityName;
    private String description;
    private Double relevanceScore;
    private Map<String, Object> entityData;
  }
}
