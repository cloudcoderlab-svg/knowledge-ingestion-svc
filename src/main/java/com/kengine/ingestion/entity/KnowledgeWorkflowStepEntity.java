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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing a step within a workflow.
 *
 * <p>Stores individual steps or actions within a workflow. Each step has a sequence number,
 * description, and optional metadata about actors, systems, or conditions.
 *
 * <p>Table: knowledge.knowledge_workflow_steps
 *
 * <p>Relationships: Many steps belong to one workflow
 */
@Entity
@Table(name = "knowledge_workflow_steps", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeWorkflowStepEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "step_id", nullable = false)
  private UUID stepId;

  @Column(name = "workflow_id", nullable = false)
  private UUID workflowId;

  @Column(name = "sequence_number")
  private Integer sequenceNumber;

  @Column(name = "step_name", length = 500)
  private String stepName;

  @Column(name = "actor", length = 255)
  private String actor;

  @Column(name = "action_text", columnDefinition = "text")
  private String actionText;

  @Column(name = "input_data", columnDefinition = "text")
  private String inputData;

  @Column(name = "output_data", columnDefinition = "text")
  private String outputData;

  @Column(name = "next_step", length = 500)
  private String nextStep;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @Column(name = "technical_details", columnDefinition = "text")
  private String technicalDetails;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "input_parameters", columnDefinition = "jsonb")
  private String inputParameters;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "output_parameters", columnDefinition = "jsonb")
  private String outputParameters;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
