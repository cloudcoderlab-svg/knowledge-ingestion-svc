package com.kengine.ingestion.entity;

import com.kengine.ingestion.model.ProcessStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Entity to track individual document processing within a process. Enables parallel processing by
 * maintaining separate status and output for each document.
 */
@Entity
@Table(
    name = "knowledge_ingestion_document_process",
    schema = "knowledge",
    indexes = {
      @Index(name = "idx_doc_proc_process_id", columnList = "process_id"),
      @Index(name = "idx_doc_proc_status", columnList = "status"),
      @Index(name = "idx_doc_proc_subject_id", columnList = "subject_id")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeIngestionDocumentProcessEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "doc_process_id", nullable = false)
  private UUID docProcessId;

  @Column(name = "process_id", nullable = false)
  private UUID processId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "file_path", nullable = false, length = 2048)
  private String filePath;

  @Column(name = "file_name", length = 512)
  private String fileName;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "mime_type", length = 255)
  private String mimeType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private ProcessStatus status;

  @Column(name = "artifact_id")
  private UUID artifactId;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @Column(name = "error_details", columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String errorDetails;

  @Column(name = "retry_count")
  private Integer retryCount;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  @Version
  @Column(name = "version")
  private Long version;
}
