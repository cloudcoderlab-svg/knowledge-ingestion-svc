package com.kengine.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "knowledge_workflows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeWorkflowEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "workflow_id", nullable = false)
  private UUID workflowId;

  @Column(name = "artifact_id", length = 36)
  private String artifactId;

  @Column(name = "project_id", nullable = false)
  private String projectId;

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

  @Column(name = "embedding", columnDefinition = "vector(768)")
  private List<Double> embedding;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
