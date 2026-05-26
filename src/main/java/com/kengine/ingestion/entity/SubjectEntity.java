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
 * Entity representing a knowledge subject.
 *
 * <p>A subject is a distinct knowledge domain or project that contains documents and extracted
 * knowledge entities. Each subject has its own GCS folder for storing source documents and an
 * isolated knowledge graph.
 *
 * <p>Table: knowledge.subjects
 *
 * <p>Examples: "customer-management-system", "payment-processing", "user-authentication"
 */
@Entity
@Table(name = "subjects", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectEntity {

  @Id
  @GeneratedValue
  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "subject_name", nullable = false, length = 500)
  private String subjectName;

  @Column(name = "version", nullable = false)
  @Builder.Default
  private Integer version = 1;

  @Column(name = "title", nullable = false, length = 500)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "source_bucket", nullable = false, length = 255)
  private String sourceBucket;

  @Column(name = "gcs_folder_url", nullable = false, columnDefinition = "TEXT")
  private String gcsFolderUrl;

  @Column(name = "status", length = 50)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private SubjectStatus status = SubjectStatus.DRAFT;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
