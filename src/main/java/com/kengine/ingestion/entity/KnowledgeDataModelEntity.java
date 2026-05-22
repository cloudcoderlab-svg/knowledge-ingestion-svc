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
@Table(name = "knowledge_data_models")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDataModelEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "data_model_id", nullable = false)
  private UUID dataModelId;

  @Column(name = "artifact_id", length = 36)
  private String artifactId;

  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(name = "domain_id")
  private UUID domainId;

  @Column(name = "model_name", length = 500, nullable = false)
  private String modelName;

  @Column(name = "model_type", length = 100)
  private String modelType;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "schema_definition", columnDefinition = "jsonb")
  private Map<String, Object> schemaDefinition;

  @Column(name = "embedding", columnDefinition = "vector(768)")
  private List<Double> embedding;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
