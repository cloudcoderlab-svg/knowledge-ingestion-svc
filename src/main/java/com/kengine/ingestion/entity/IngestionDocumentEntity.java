package com.kengine.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "ingestion_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionDocumentEntity {

  @Id
  @Column(name = "document_id", length = 36, nullable = false)
  private String documentId;

  @Column(name = "artifact_id", length = 36, nullable = false)
  private String artifactId;

  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(name = "source_bucket", nullable = false)
  private String sourceBucket;

  @Column(name = "source_object", nullable = false)
  private String sourceObject;

  @Column(name = "source_generation")
  private Long sourceGeneration;

  @Column(name = "source_checksum")
  private String sourceChecksum;

  @Column(name = "content_hash", length = 64, nullable = false)
  private String contentHash;

  @Column(name = "chunk_count", nullable = false)
  private Long chunkCount;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
