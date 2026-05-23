package com.kengine.ingestion.entity;

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
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_data_fields")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDataFieldEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "field_id", nullable = false)
  private UUID fieldId;

  @Column(name = "data_model_id", nullable = false)
  private UUID dataModelId;

  @Column(name = "field_name", length = 255, nullable = false)
  private String fieldName;

  @Column(name = "field_type", length = 100)
  private String fieldType;

  @Column(name = "is_required")
  private Boolean isRequired;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "constraints", columnDefinition = "jsonb")
  private Map<String, Object> constraints;

  @Column(name = "embedding", columnDefinition = "vector(768)")
  private java.util.List<Double> embedding;

  @Column(name = "business_name", length = 500)
  private String businessName;

  @Column(name = "business_definition", columnDefinition = "text")
  private String businessDefinition;

  @Column(name = "business_examples", columnDefinition = "text")
  private String businessExamples;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "business_rules", columnDefinition = "jsonb")
  private Map<String, Object> businessRules;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
