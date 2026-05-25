package com.kengine.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "document_knowledge")
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

  @Column(name = "key_entities", columnDefinition = "text[]")
  private String[] keyEntities;

  @Column(name = "key_concepts", columnDefinition = "text[]")
  private String[] keyConcepts;

  @Column(name = "technologies", columnDefinition = "text[]")
  private String[] technologies;

  @Column(name = "extracted_at")
  private OffsetDateTime extractedAt;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
