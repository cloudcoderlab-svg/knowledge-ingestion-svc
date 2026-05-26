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
 * Entity representing a relationship between knowledge entities.
 *
 * <p>Stores directed edges in the knowledge graph connecting components, workflows, business rules,
 * data models, and other entities. Relationships have types and optional metadata.
 *
 * <p>Table: knowledge.knowledge_relationships
 *
 * <p>Examples: "Component A depends on Component B", "Workflow X invokes API Y"
 */
@Entity
@Table(name = "knowledge_relationships", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRelationshipEntity {

  @Id
  @Column(name = "relationship_id", length = 36, nullable = false)
  private String relationshipId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "source_name", nullable = false)
  private String sourceName;

  @Column(name = "source_type", nullable = false)
  private String sourceType;

  @Column(name = "source_ref_id", length = 36)
  private String sourceRefId;

  @Column(name = "target_name", nullable = false)
  private String targetName;

  @Column(name = "target_type")
  private String targetType;

  @Column(name = "target_ref_id", length = 36)
  private String targetRefId;

  @Column(name = "relationship_type", nullable = false)
  private String relationshipType;

  @Column(name = "context", columnDefinition = "text")
  private String context;

  @Column(name = "source_artifact_id")
  private UUID sourceArtifactId;

  @Column(name = "source_chunk_id")
  private UUID sourceChunkId;

  @Column(name = "confidence")
  private Double confidence;

  // New fields from 005-redesign-phase1-schema
  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @Column(name = "source_entity_type", length = 100)
  private String sourceEntityType;

  @Column(name = "target_entity_type", length = 100)
  private String targetEntityType;

  @Column(name = "source_entity_id")
  private UUID sourceEntityId;

  @Column(name = "target_entity_id")
  private UUID targetEntityId;

  @Column(name = "business_relationship_type", length = 100)
  private String businessRelationshipType;

  @Column(name = "business_description", columnDefinition = "text")
  private String businessDescription;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
