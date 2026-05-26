package com.kengine.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entity representing a parsed ingestion document.
 *
 * <p>Stores the raw parsed content and metadata extracted from source files during document
 * processing. Serves as an intermediate representation before knowledge extraction.
 *
 * <p>Table: knowledge.ingestion_documents
 */
@Entity
@Table(name = "ingestion_documents", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionDocumentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "artifact_id")
  private UUID artifactId;

  @Column(name = "document_name", length = 500)
  private String documentName;

  @Column(name = "document_type", length = 100)
  private String documentType;

  @Column(name = "gcs_url", columnDefinition = "text")
  private String gcsUrl;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "mime_type", length = 255)
  private String mimeType;

  @Column(name = "source_bucket", length = 255)
  private String sourceBucket;

  @Column(name = "source_object", columnDefinition = "text")
  private String sourceObject;

  @Column(name = "content_hash", length = 64)
  private String contentHash;

  @Column(name = "source_checksum", length = 64)
  private String sourceChecksum;

  @Column(name = "source_generation")
  private Long sourceGeneration;

  @Column(name = "chunk_count")
  private Integer chunkCount;

  @Column(name = "extraction_status", length = 50)
  private String extractionStatus;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @Column(name = "metadata", columnDefinition = "jsonb")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  private String metadata;

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
