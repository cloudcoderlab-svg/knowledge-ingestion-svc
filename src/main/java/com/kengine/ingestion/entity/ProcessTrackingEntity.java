package com.kengine.ingestion.entity;

import com.kengine.ingestion.model.ProcessStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_ingestion_process", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTrackingEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "process_id", nullable = false)
  private UUID processId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "process_type", length = 50)
  private String processType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private ProcessStatus status;

  @Column(name = "total_files")
  private Integer totalFiles;

  @Column(name = "processed_files")
  private Integer processedFiles;

  @Column(name = "failed_files")
  private Integer failedFiles;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "file_list", columnDefinition = "jsonb")
  private List<String> fileList;

  @Column(name = "current_file", length = 2048)
  private String currentFile;

  @Column(name = "failure_cause", columnDefinition = "text")
  private String failureCause;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "error_details", columnDefinition = "jsonb")
  private Map<String, Object> errorDetails;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
