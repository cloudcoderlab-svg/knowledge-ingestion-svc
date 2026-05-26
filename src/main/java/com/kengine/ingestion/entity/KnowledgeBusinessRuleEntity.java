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

/**
 * Entity representing a business rule.
 *
 * <p>Stores business rules, policies, constraints, validations, and decision-making criteria
 * extracted from documentation. Includes condition-outcome pairs and vector embeddings for semantic
 * search.
 *
 * <p>Table: knowledge.knowledge_business_rules
 *
 * <p>Examples: validation rules, approval policies, pricing constraints
 */
@Entity
@Table(name = "knowledge_business_rules", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBusinessRuleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "rule_id", nullable = false)
  private UUID ruleId;

  @Column(name = "artifact_id")
  private UUID artifactId;

  @Column(name = "component_id")
  private UUID componentId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "domain_id")
  private UUID domainId;

  @Column(name = "rule_name", length = 500, nullable = false)
  private String ruleName;

  @Column(name = "rule_type", length = 100)
  private String ruleType;

  @Column(name = "condition_text", columnDefinition = "text")
  private String conditionText;

  @Column(name = "outcome_text", columnDefinition = "text")
  private String outcomeText;

  @Column(name = "priority", length = 50)
  private String priority;

  @Column(name = "confidence")
  private Double confidence;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @Column(name = "technical_implementation", columnDefinition = "text")
  private String technicalImplementation;

  @Column(name = "validation_criteria", columnDefinition = "text")
  private String validationCriteria;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
