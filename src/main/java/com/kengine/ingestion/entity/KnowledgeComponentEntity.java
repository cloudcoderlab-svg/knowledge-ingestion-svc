package com.kengine.ingestion.entity;

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
@Table(name = "knowledge_components")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeComponentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "component_id", nullable = false)
  private UUID componentId;

  @Column(name = "artifact_id", length = 36)
  private String artifactId;

  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(name = "domain_id")
  private UUID domainId;

  @Column(name = "subdomain_id")
  private UUID subdomainId;

  @Column(name = "component_name", length = 500, nullable = false)
  private String componentName;

  @Column(name = "component_type", length = 100)
  private String componentType;

  @Column(name = "category", length = 50)
  private String category;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "responsibility", columnDefinition = "text")
  private String responsibility;

  @Column(name = "technology", length = 255)
  private String technology;

  @Column(name = "capability", length = 500)
  private String capability;

  @Column(name = "owner", length = 255)
  private String owner;

  @Column(name = "lifecycle", length = 50)
  private String lifecycle;

  @Column(name = "confidence")
  private Double confidence;

  @Column(name = "embedding", columnDefinition = "vector(768)")
  private List<Double> embedding;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
