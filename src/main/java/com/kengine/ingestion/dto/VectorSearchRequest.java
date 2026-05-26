package com.kengine.ingestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for vector similarity search.
 *
 * <p>Supports both text-based search (with automatic embedding generation) and direct embedding
 * vector search. Exactly one of queryText or embedding must be provided.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description =
        "Vector search request supporting text queries or direct embedding vectors. "
            + "Provide either queryText OR embedding, not both.")
public class VectorSearchRequest {

  @Schema(
      description =
          "Text query to search for. Will be automatically converted to embedding vector.",
      example = "How does the authentication process work?")
  private String queryText;

  @Size(min = 768, max = 768, message = "Embedding must be exactly 768 dimensions")
  @Schema(
      description = "Pre-computed embedding vector (768 dimensions). Alternative to queryText.",
      example = "[0.1, 0.2, -0.3, ...]",
      minLength = 768,
      maxLength = 768)
  private List<Double> embedding;

  @Schema(
      description = "Filter results by source file",
      example = "subjects/customer-mgmt/architecture.pdf")
  private String sourceObject;

  @Schema(description = "Filter results by domain", example = "authentication")
  private String domain;

  @Min(value = 1, message = "Limit must be at least 1")
  @Max(value = 100, message = "Limit must not exceed 100")
  @Schema(
      description = "Maximum number of results to return",
      example = "10",
      minimum = "1",
      maximum = "100",
      defaultValue = "10")
  private Integer limit;

  @Min(value = 0, message = "Threshold must be between 0 and 1")
  @Max(value = 1, message = "Threshold must be between 0 and 1")
  @Schema(
      description = "Minimum similarity score threshold (0.0 to 1.0)",
      example = "0.7",
      minimum = "0",
      maximum = "1")
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
