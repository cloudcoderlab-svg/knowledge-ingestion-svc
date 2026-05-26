package com.kengine.ingestion.dto;

import com.kengine.ingestion.entity.SubjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing subject details.
 *
 * <p>Includes metadata, GCS folder information, and URLs for accessing subject resources.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subject details including metadata and GCS folder information")
public class SubjectResponse {

  @Schema(
      description = "Unique identifier of the subject",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID subjectId;

  @Schema(
      description = "Machine-friendly identifier for the subject",
      example = "customer-management-system")
  private String subjectName;

  @Schema(description = "Version number of the subject", example = "1")
  private Integer version;

  @Schema(
      description = "Human-readable title",
      example = "Customer Management System Documentation")
  private String title;

  @Schema(
      description = "Detailed description of the subject",
      example = "Documentation and knowledge base for the customer management system")
  private String description;

  @Schema(
      description = "GCS bucket name where files are stored",
      example = "kengine-knowledge-artifacts")
  private String sourceBucket;

  @Schema(
      description = "Full GCS URL to the subject's folder",
      example = "gs://kengine-knowledge-artifacts/subjects/customer-management-system/")
  private String gcsFolderUrl;

  @Schema(
      description = "URL for uploading files (if available)",
      example = "https://storage.googleapis.com/...")
  private String uploadUrl;

  @Schema(
      description = "URL to the subject's definition.md file",
      example =
          "gs://kengine-knowledge-artifacts/subjects/customer-management-system/definition.md")
  private String definitionUrl;

  @Schema(
      description = "Current lifecycle status of the subject",
      example = "ACTIVE",
      allowableValues = {"DRAFT", "INGESTING", "ACTIVE", "FAILED"})
  private SubjectStatus status;

  @Schema(
      description = "Timestamp when the subject was created",
      example = "2026-05-26T10:30:00+05:30")
  private OffsetDateTime createdAt;

  @Schema(
      description = "Timestamp when the subject was last updated",
      example = "2026-05-26T10:30:00+05:30")
  private OffsetDateTime updatedAt;
}
