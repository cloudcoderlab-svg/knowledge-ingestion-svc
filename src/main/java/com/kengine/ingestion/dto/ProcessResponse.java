package com.kengine.ingestion.dto;

import com.kengine.ingestion.model.ProcessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing process status and progress information.
 *
 * <p>Tracks the progress of document ingestion jobs including file counts, errors, and completion
 * status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Processing job status and progress information")
public class ProcessResponse {

  @Schema(
      description = "Unique identifier of the process",
      example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
  private UUID processId;

  @Schema(
      description = "Subject this process belongs to",
      example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID subjectId;

  @Schema(description = "Type of processing operation", example = "document_ingestion")
  private String processType;

  @Schema(
      description = "Current status of the process",
      example = "IN_PROGRESS",
      allowableValues = {"INIT", "IN_PROGRESS", "SUCCESS", "FAILED"})
  private ProcessStatus status;

  @Schema(description = "Total number of files to process", example = "24")
  private Integer totalFiles;

  @Schema(description = "Number of files successfully processed", example = "18")
  private Integer processedFiles;

  @Schema(description = "Number of files that failed processing", example = "2")
  private Integer failedFiles;

  @Schema(
      description = "File currently being processed",
      example = "subjects/customer-mgmt/user-guide.pdf")
  private String currentFile;

  @Schema(description = "Error message if process failed", example = "Failed to parse document")
  private String failureCause;

  @Schema(description = "Detailed error information including stack traces")
  private Map<String, Object> errorDetails;

  @Schema(description = "List of all files in this process")
  private List<String> fileList;

  @Schema(description = "Completion percentage (0-100)", example = "75.0")
  private Double progress;

  @Schema(description = "When the process started", example = "2026-05-26T10:30:00+05:30")
  private OffsetDateTime startedAt;

  @Schema(description = "When the process completed", example = "2026-05-26T11:00:00+05:30")
  private OffsetDateTime completedAt;

  @Schema(description = "When the process was created", example = "2026-05-26T10:30:00+05:30")
  private OffsetDateTime createdAt;

  @Schema(description = "When the process was last updated", example = "2026-05-26T10:45:00+05:30")
  private OffsetDateTime updatedAt;

  public Double getProgress() {
    if (totalFiles == null || totalFiles == 0) {
      return 0.0;
    }
    return (processedFiles != null ? processedFiles.doubleValue() : 0.0)
        / totalFiles.doubleValue()
        * 100.0;
  }
}
