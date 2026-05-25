package com.kengine.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedUrlResponse {

  private String signedUrl;
  private String fileName;
  private String gcsPath;
  private Integer expiresInMinutes;
}
