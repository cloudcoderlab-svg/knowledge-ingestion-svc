package com.kengine.ingestion.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchResponse<T> {
  private List<SearchResult<T>> results;
  private Integer totalResults;
  private String searchType; // "text" or "embedding"
  private String entityType; // "semantic_chunk", "component", "business_rule", "workflow"
}
