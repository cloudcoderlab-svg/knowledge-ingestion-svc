package com.kengine.ingestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper for search results with similarity scores.
 *
 * @param <T> The type of entity returned in the result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Individual search result with similarity score")
public class SearchResult<T> {

  @Schema(description = "The matched entity")
  private T entity;

  @Schema(
      description =
          "Cosine similarity score between query and result (0.0 to 1.0, higher is more similar)",
      example = "0.87",
      minimum = "0",
      maximum = "1")
  private Double similarityScore;

  @Schema(description = "Type of the entity", example = "semantic_chunk")
  private String entityType;
}
