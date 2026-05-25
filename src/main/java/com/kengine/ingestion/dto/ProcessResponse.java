package com.kengine.ingestion.dto;

import com.kengine.ingestion.model.ProcessStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResponse {

  private UUID processId;
  private UUID subjectId;
  private String processType;
  private ProcessStatus status;
  private Integer totalFiles;
  private Integer processedFiles;
  private Integer failedFiles;
  private String currentFile;
  private String failureCause;
  private Map<String, Object> errorDetails;
  private List<String> fileList;
  private Double progress;
  private OffsetDateTime startedAt;
  private OffsetDateTime completedAt;
  private OffsetDateTime createdAt;
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
