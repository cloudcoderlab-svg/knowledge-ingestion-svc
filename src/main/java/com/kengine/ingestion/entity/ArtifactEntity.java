package com.kengine.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "artifacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactEntity {

  @Id
  @Column(name = "artifact_id", length = 36, nullable = false)
  private String artifactId;

  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(name = "domain")
  private String domain;

  @Column(name = "subdomain")
  private String subdomain;

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

  @Column(name = "artifact_type", nullable = false)
  private String artifactType;

  @Column(name = "file_type", nullable = false)
  private String fileType;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "version")
  private String version;

  @Column(name = "is_current", nullable = false)
  private Boolean isCurrent;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
