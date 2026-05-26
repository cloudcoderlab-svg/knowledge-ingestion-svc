package com.kengine.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entity representing document-level knowledge summary.
 *
 * <p>Stores high-level summaries and metadata about processed documents, including key entities,
 * topics, document type, and characteristics extracted during processing.
 *
 * <p>Table: knowledge.document_knowledge
 */
@Entity
@Table(name = "document_knowledge", schema = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentKnowledgeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "doc_knowledge_id", nullable = false)
  private UUID docKnowledgeId;

  @Column(name = "artifact_id", nullable = false)
  private UUID artifactId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "title", length = 1000)
  private String title;

  @Column(name = "summary", columnDefinition = "text")
  private String summary;

  @Column(name = "domain", length = 500)
  private String domain;

  @Column(name = "subdomain", length = 500)
  private String subdomain;

  @Column(name = "document_type", length = 100)
  private String documentType;

  @Column(name = "overall_architecture", columnDefinition = "text")
  private String overallArchitecture;

  @Column(name = "key_entities", columnDefinition = "text[]")
  private String[] keyEntities;

  @Column(name = "key_concepts", columnDefinition = "text[]")
  private String[] keyConcepts;

  @Column(name = "technologies", columnDefinition = "text[]")
  private String[] technologies;

  @Column(name = "identified_components", columnDefinition = "text[]")
  private String[] identifiedComponents;

  @Column(name = "identified_apis", columnDefinition = "text[]")
  private String[] identifiedApis;

  @Column(name = "identified_workflows", columnDefinition = "text[]")
  private String[] identifiedWorkflows;

  @Column(name = "identified_capabilities", columnDefinition = "text[]")
  private String[] identifiedCapabilities;

  @Column(name = "identified_roles", columnDefinition = "text[]")
  private String[] identifiedRoles;

  @Column(name = "identified_terms", columnDefinition = "text[]")
  private String[] identifiedTerms;

  @Column(name = "identified_policies", columnDefinition = "text[]")
  private String[] identifiedPolicies;

  @Column(name = "identified_decisions", columnDefinition = "text[]")
  private String[] identifiedDecisions;

  @Column(name = "extracted_at")
  private OffsetDateTime extractedAt;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
