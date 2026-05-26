package com.kengine.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entity representing a project.
 *
 * <p>Stores project metadata for organizing and categorizing subjects. Projects can contain
 * multiple subjects.
 *
 * <p>Table: knowledge.projects
 */
@Entity
@Table(name = "projects", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntity {

  @Id
  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(name = "project_name", nullable = false)
  private String projectName;

  @Column(name = "source_bucket")
  private String sourceBucket;

  @Column(name = "gcs_prefix")
  private String gcsPrefix;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
