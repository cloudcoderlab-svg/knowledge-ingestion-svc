package com.kengine.ingestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new subject.
 *
 * <p>A subject represents a knowledge domain or project that contains documents to be ingested and
 * analyzed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new subject with its metadata")
public class CreateSubjectRequest {

  @NotBlank(message = "Title is required")
  @Size(max = 500, message = "Title must not exceed 500 characters")
  @Schema(
      description = "Human-readable title for the subject",
      example = "Customer Management System Documentation",
      required = true,
      maxLength = 500)
  private String title;

  @NotBlank(message = "Description is required")
  @Schema(
      description = "Detailed description of the subject's scope and purpose",
      example =
          "Documentation and knowledge base for the customer management system including architecture, APIs, and business rules",
      required = true)
  private String description;

  @Size(max = 500, message = "Subject name must not exceed 500 characters")
  @Schema(
      description =
          "Optional machine-friendly identifier for the subject. If not provided, generated from title",
      example = "customer-management-system",
      maxLength = 500)
  private String subjectName;
}
