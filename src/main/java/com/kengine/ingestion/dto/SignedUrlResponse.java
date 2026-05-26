package com.kengine.ingestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing a signed URL for file upload.
 *
 * <p>Includes the signed URL, target path, and expiration information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Signed URL response for file upload")
public class SignedUrlResponse {

  @Schema(
      description = "Time-limited signed URL for uploading the file via PUT request",
      example = "https://storage.googleapis.com/bucket/path/to/file?X-Goog-Algorithm=...")
  private String signedUrl;

  @Schema(description = "Name of the file", example = "architecture-diagram.pdf")
  private String fileName;

  @Schema(
      description = "Full GCS path where the file will be stored",
      example = "gs://kengine-knowledge-artifacts/subjects/customer-mgmt/architecture-diagram.pdf")
  private String gcsPath;

  @Schema(description = "Number of minutes until the URL expires", example = "60")
  private Integer expiresInMinutes;
}
