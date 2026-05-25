package com.kengine.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedUrlRequest {

  @NotBlank(message = "File name is required")
  private String fileName;

  @NotBlank(message = "Content type is required")
  private String contentType;

  @Positive @Builder.Default private Integer expirationMinutes = 60;
}
