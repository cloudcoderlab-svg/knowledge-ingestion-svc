package com.kengine.ingestion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchRequest {

  private String queryText;

  @Size(min = 768, max = 768, message = "Embedding must be exactly 768 dimensions")
  private List<Double> embedding;

  private String sourceObject;

  private String domain;

  @Min(value = 1, message = "Limit must be at least 1")
  @Max(value = 100, message = "Limit must not exceed 100")
  private Integer limit;

  @Min(value = 0, message = "Threshold must be between 0 and 1")
  @Max(value = 1, message = "Threshold must be between 0 and 1")
  private Double threshold;

  public boolean hasQueryText() {
    return queryText != null && !queryText.isBlank();
  }

  public boolean hasEmbedding() {
    return embedding != null && !embedding.isEmpty();
  }

  public boolean isValid() {
    return hasQueryText() ^ hasEmbedding(); // XOR: exactly one must be present
  }
}
