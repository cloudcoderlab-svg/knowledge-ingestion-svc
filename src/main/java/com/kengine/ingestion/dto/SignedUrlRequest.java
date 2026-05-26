package com.kengine.ingestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for generating a signed URL for file upload.
 *
 * <p>Signed URLs allow direct upload to GCS without requiring authentication credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to generate a signed URL for uploading files to GCS")
public class SignedUrlRequest {

  @NotBlank(message = "File name is required")
  @Schema(
      description = "Name of the file to upload",
      example = "architecture-diagram.pdf",
      required = true)
  private String fileName;

  @NotBlank(message = "Content type is required")
  @Schema(description = "MIME type of the file", example = "application/pdf", required = true)
  private String contentType;

  @Positive
  @Builder.Default
  @Schema(
      description = "URL expiration time in minutes",
      example = "60",
      defaultValue = "60",
      minimum = "1")
  private Integer expirationMinutes = 60;
}
