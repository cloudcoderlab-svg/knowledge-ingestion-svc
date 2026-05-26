package com.kengine.ingestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for vector similarity search results.
 *
 * <p>Contains ranked search results with similarity scores and metadata about the search.
 *
 * @param <T> The type of search result entity (SemanticChunkSearchResult, ComponentSearchResult,
 *     etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vector search response with ranked results and metadata")
public class VectorSearchResponse<T> {

  @Schema(description = "List of search results ranked by similarity score")
  private List<SearchResult<T>> results;

  @Schema(description = "Total number of results found", example = "42")
  private Integer totalResults;

  @Schema(
      description = "Type of search performed",
      example = "text",
      allowableValues = {"text", "embedding"})
  private String searchType; // "text" or "embedding"

  @Schema(
      description = "Type of entities searched",
      example = "semantic_chunk",
      allowableValues = {"semantic_chunk", "component", "business_rule", "workflow"})
  private String entityType; // "semantic_chunk", "component", "business_rule", "workflow"
}
