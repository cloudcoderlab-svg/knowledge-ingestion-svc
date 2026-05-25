package com.kengine.ingestion.entity;

import com.kengine.ingestion.config.VectorType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
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

  @Column(name = "artifact_id")
  private UUID artifactId;

  @Column(name = "component_id")
  private UUID componentId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "domain_id")
  private UUID domainId;

  @Column(name = "model_name", length = 500, nullable = false)
  private String modelName;

  @Column(name = "model_type", length = 100)
  private String modelType;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "database_type", length = 100)
  private String databaseType;

  @Column(name = "schema_name", length = 255)
  private String schemaName;

  @Column(name = "confidence")
  private Double confidence;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "schema_definition", columnDefinition = "jsonb")
  private Map<String, Object> schemaDefinition;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @Column(name = "business_name", length = 500)
  private String businessName;

  @Column(name = "business_definition", columnDefinition = "text")
  private String businessDefinition;

  @Column(name = "business_owner", length = 255)
  private String businessOwner;

  @Column(name = "data_sensitivity", length = 100)
  private String dataSensitivity;

  @Column(name = "data_quality_requirements", columnDefinition = "text")
  private String dataQualityRequirements;

  @Column(name = "business_usage", columnDefinition = "text")
  private String businessUsage;

  @Column(name = "master_data")
  private Boolean masterData;

  @Column(name = "reference_data")
  private Boolean referenceData;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
