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

/**
 * Entity representing a data field within a data model.
 *
 * <p>Stores individual fields, columns, or attributes within data models. Includes metadata about
 * data types, constraints, nullability, and relationships.
 *
 * <p>Table: knowledge.knowledge_data_fields
 *
 * <p>Relationships: Many fields belong to one data model
 */
@Entity
@Table(name = "knowledge_data_fields", schema = "knowledge")
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

  @Column(name = "is_primary_key")
  private Boolean isPrimaryKey;

  @Column(name = "is_nullable")
  private Boolean isNullable;

  @Column(name = "is_required")
  private Boolean isRequired;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "constraints", columnDefinition = "jsonb")
  private Map<String, Object> constraints;

  @Column(name = "business_name", length = 255)
  private String businessName;

  @Column(name = "business_definition", columnDefinition = "text")
  private String businessDefinition;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "business_rules", columnDefinition = "jsonb")
  private Map<String, Object> businessRules;

  @Column(name = "business_examples", columnDefinition = "text")
  private String businessExamples;

  @Column(name = "data_sensitivity", length = 100)
  private String dataSensitivity;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
