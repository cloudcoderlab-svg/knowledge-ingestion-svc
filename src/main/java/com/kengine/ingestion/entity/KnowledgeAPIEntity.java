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
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "knowledge_apis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeAPIEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "api_id", nullable = false)
  private UUID apiId;

  @Column(name = "component_id")
  private UUID componentId;

  @Column(name = "artifact_id")
  private UUID artifactId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "api_name", length = 500, nullable = false)
  private String apiName;

  @Column(name = "api_type", length = 100)
  private String apiType;

  @Column(name = "http_method", length = 10)
  private String httpMethod;

  @Column(name = "endpoint_path", length = 2000)
  private String endpointPath;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "request_schema", columnDefinition = "jsonb")
  private Map<String, Object> requestSchema;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "response_schema", columnDefinition = "jsonb")
  private Map<String, Object> responseSchema;

  @Column(name = "authentication", length = 100)
  private String authentication;

  @Column(name = "embedding")
  @Type(VectorType.class)
  private String embedding; // pgvector handled by custom UserType

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;
}
