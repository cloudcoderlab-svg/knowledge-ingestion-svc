package com.kengine.ingestion.entity;

import com.kengine.ingestion.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "semantic_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticChunkEntity {

  @Id
  @Column(name = "chunk_id", nullable = false)
  private UUID chunkId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @Column(name = "artifact_id")
  private UUID artifactId;

  @Column(name = "domain_id")
  private UUID domainId;

  @Column(name = "subdomain_id")
  private UUID subdomainId;

  @Column(name = "content", columnDefinition = "text", nullable = false)
  private String content;

  @Column(name = "chunk_index", nullable = false)
  private Integer chunkIndex;

  @Column(name = "total_chunks")
  private Long totalChunks;

  @Column(name = "chunk_size")
  private Integer chunkSize;

  @Column(name = "char_start")
  private Long charStart;

  @Column(name = "char_end")
  private Long charEnd;

  @Column(name = "domain", length = 500)
  private String domain;

  @Column(name = "subdomain", length = 500)
  private String subdomain;

  @Column(name = "source_bucket", length = 255)
  private String sourceBucket;

  @Column(name = "source_object", columnDefinition = "text")
  private String sourceObject;

  @Column(name = "source_generation")
  private Long sourceGeneration;

  @Column(name = "source_checksum", length = 64)
  private String sourceChecksum;

  @Column(name = "document_content_hash", length = 64)
  private String documentContentHash;

  @Column(name = "chunk_content_hash", length = 64)
  private String chunkContentHash;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @Column(name = "embedding_status", length = 50)
  private String embeddingStatus;

  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
