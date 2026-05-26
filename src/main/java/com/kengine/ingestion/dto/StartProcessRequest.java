package com.kengine.ingestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for starting a document processing job.
 *
 * <p>Optionally specifies a subset of files to process. If not provided, all files in the subject's
 * folder will be processed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to start a document ingestion process")
public class StartProcessRequest {

  @Schema(
      description =
          "Optional list of specific file paths to process. If omitted, all files in the subject folder will be processed.",
      example = "[\"document1.pdf\", \"document2.docx\"]")
  private List<String> files;
}
