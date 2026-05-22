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
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeResourceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "resource_id", nullable = false)
  private UUID resourceId;

  @Column(name = "artifact_id", length = 36)
  private String artifactId;

  @Column(name = "component_id")
  private UUID componentId;

  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(name = "resource_name", length = 500, nullable = false)
  private String resourceName;

  @Column(name = "resource_type", length = 100)
  private String resourceType;

  @Column(name = "provider", length = 100)
  private String provider;

  @Column(name = "hosting_model", length = 50)
  private String hostingModel;

  @Column(name = "environment", length = 50)
  private String environment;

  @Column(name = "region", length = 100)
  private String region;

  @Column(name = "criticality", length = 50)
  private String criticality;

  @Column(name = "lifecycle", length = 50)
  private String lifecycle;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "configs", columnDefinition = "jsonb")
  private Map<String, Object> configs;

  @Column(name = "confidence")
  private Double confidence;

  @Column(name = "embedding", columnDefinition = "vector(768)")
  private List<Double> embedding;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
