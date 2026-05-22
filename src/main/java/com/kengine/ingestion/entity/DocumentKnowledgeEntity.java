package com.kengine.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  @Column(name = "artifact_id", length = 36, nullable = false)
  private String artifactId;

  @Column(name = "overall_architecture", columnDefinition = "text")
  private String overallArchitecture;

  @Column(name = "system_summary", columnDefinition = "text")
  private String systemSummary;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "key_patterns", columnDefinition = "jsonb")
  private Map<String, Object> keyPatterns;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "technologies", columnDefinition = "jsonb")
  private Map<String, Object> technologies;

  @Column(name = "embedding", columnDefinition = "vector(768)")
  private List<Double> embedding;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
