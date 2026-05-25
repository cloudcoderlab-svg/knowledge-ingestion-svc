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

@Entity
@Table(name = "knowledge_integrations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeIntegrationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "integration_id", nullable = false)
  private UUID integrationId;

  @Column(name = "artifact_id")
  private UUID artifactId;

  @Column(name = "component_id")
  private UUID componentId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "integration_name", length = 500, nullable = false)
  private String integrationName;

  @Column(name = "integration_type", length = 100)
  private String integrationType;

  @Column(name = "source_system", length = 255)
  private String sourceSystem;

  @Column(name = "target_system", length = 255)
  private String targetSystem;

  @Column(name = "protocol", length = 100)
  private String protocol;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "confidence")
  private Double confidence;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
