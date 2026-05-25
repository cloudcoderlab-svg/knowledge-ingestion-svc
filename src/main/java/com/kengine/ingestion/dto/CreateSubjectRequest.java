package com.kengine.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubjectRequest {

  @NotBlank(message = "Title is required")
  @Size(max = 500, message = "Title must not exceed 500 characters")
  private String title;

  @NotBlank(message = "Description is required")
  private String description;

  @Size(max = 500, message = "Subject name must not exceed 500 characters")
  private String subjectName;
}
