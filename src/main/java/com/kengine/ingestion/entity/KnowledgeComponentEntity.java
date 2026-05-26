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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing an architectural component.
 *
 * <p>Stores information about system building blocks such as services, libraries, databases, APIs,
 * and other architectural elements extracted from documentation.
 *
 * <p>Table: knowledge.knowledge_components
 *
 * <p>Includes vector embeddings for semantic similarity search of components.
 */
@Entity
@Table(name = "knowledge_components", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeComponentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "component_id", nullable = false)
  private UUID componentId;

  @Column(name = "artifact_id")
  private UUID artifactId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "domain_id")
  private UUID domainId;

  @Column(name = "subdomain_id")
  private UUID subdomainId;

  @Column(name = "component_name", length = 500, nullable = false)
  private String componentName;

  @Column(name = "component_type", length = 100)
  private String componentType;

  @Column(name = "category", length = 50)
  private String category;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "responsibility", columnDefinition = "text")
  private String responsibility;

  @Column(name = "technology", length = 255)
  private String technology;

  @Column(name = "capability", length = 500)
  private String capability;

  @Column(name = "owner", length = 255)
  private String owner;

  @Column(name = "lifecycle", length = 50)
  private String lifecycle;

  @Column(name = "confidence")
  private Double confidence;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
