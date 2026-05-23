package com.kengine.ingestion.entity;

import com.kengine.ingestion.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "semantic_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticChunkEntity {

  @Id
  @Column(name = "chunk_id", length = 36, nullable = false)
  private String chunkId;

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

  @Column(name = "document_content_hash", length = 64, nullable = false)
  private String documentContentHash;

  @Column(name = "chunk_index", nullable = false)
  private Long chunkIndex;

  @Column(name = "total_chunks", nullable = false)
  private Long totalChunks;

  @Column(name = "char_start", nullable = false)
  private Long charStart;

  @Column(name = "char_end", nullable = false)
  private Long charEnd;

  @Column(name = "chunk_content_hash", length = 64, nullable = false)
  private String chunkContentHash;

  @Column(name = "domain")
  private String domain;

  @Column(name = "subdomain")
  private String subdomain;

  @Column(name = "content", columnDefinition = "text")
  private String content;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
