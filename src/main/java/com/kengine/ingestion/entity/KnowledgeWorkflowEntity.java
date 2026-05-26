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
 * Entity representing a business workflow or process.
 *
 * <p>Stores workflows, procedures, sequences of actions, and operational flows extracted from
 * documentation. Includes trigger conditions, outcomes, and vector embeddings for semantic search.
 *
 * <p>Table: knowledge.knowledge_workflows
 *
 * <p>Examples: order processing workflow, user onboarding process, payment reconciliation
 */
@Entity
@Table(name = "knowledge_workflows", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeWorkflowEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "workflow_id", nullable = false)
  private UUID workflowId;

  @Column(name = "artifact_id")
  private UUID artifactId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "domain_id")
  private UUID domainId;

  @Column(name = "workflow_name", length = 500, nullable = false)
  private String workflowName;

  @Column(name = "trigger_text", columnDefinition = "text")
  private String triggerText;

  @Column(name = "outcome_text", columnDefinition = "text")
  private String outcomeText;

  @Column(name = "owner", length = 255)
  private String owner;

  @Column(name = "confidence")
  private Double confidence;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
