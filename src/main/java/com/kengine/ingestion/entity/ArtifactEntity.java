package com.kengine.ingestion.entity;

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
 * Entity representing a source document artifact.
 *
 * <p>Represents source documents stored in GCS that have been processed and ingested. Tracks
 * metadata, content hash for deduplication, and processing status.
 *
 * <p>Table: knowledge.artifacts
 *
 * <p>Unique constraint: (subject_id, source_bucket, source_object, content_hash)
 */
@Entity
@Table(name = "artifacts", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactEntity {

  @Id
  @Column(name = "artifact_id", nullable = false)
  private UUID artifactId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "artifact_name", length = 500)
  private String artifactName;

  @Column(name = "artifact_type", length = 100)
  private String artifactType;

  @Column(name = "description", columnDefinition = "text")
  private String description;

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

  @Column(name = "source_checksum", length = 64)
  private String sourceChecksum;

  @Column(name = "source_generation")
  private Long sourceGeneration;

  @Column(name = "content_hash", length = 64, nullable = false)
  private String contentHash;

  @Column(name = "domain", length = 500)
  private String domain;

  @Column(name = "subdomain", length = 500)
  private String subdomain;

  @Column(name = "file_type", length = 100)
  private String fileType;

  @Column(name = "title", length = 1000)
  private String title;

  @Column(name = "version")
  private Integer version;

  @Column(name = "is_current")
  private Boolean isCurrent;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
