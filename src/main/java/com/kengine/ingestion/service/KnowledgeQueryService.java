package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.GenericKnowledgeSnapshot;
import com.kengine.ingestion.dto.KnowledgeSource;
import com.kengine.ingestion.dto.SourceChunk;
import com.kengine.ingestion.entity.ArtifactEntity;
import com.kengine.ingestion.entity.SemanticChunkEntity;
import com.kengine.ingestion.repository.ArtifactRepository;
import com.kengine.ingestion.repository.SemanticChunkRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryService {

  private static final int DEFAULT_CHUNK_LIMIT = 100;
  private static final int MAX_CHUNK_LIMIT = 1000;

  private final ArtifactRepository artifactRepository;
  private final SemanticChunkRepository semanticChunkRepository;
  private final NamedParameterJdbcTemplate jdbc; // Keep for legacy tables

  @PersistenceContext private EntityManager entityManager;

  public List<KnowledgeSource> sources(String projectId) {
    List<ArtifactEntity> artifacts =
        artifactRepository.findAll(
            (root, query, cb) -> cb.equal(root.get("projectId"), projectId),
            Sort.by(Sort.Direction.DESC, "sourceObject", "createdAt"));

    return artifacts.stream()
        .map(
            artifact ->
                new KnowledgeSource(
                    artifact.getArtifactId(),
                    artifact.getProjectId(),
                    artifact.getDomain(),
                    artifact.getSubdomain(),
                    artifact.getSourceBucket(),
                    artifact.getSourceObject(),
                    artifact.getSourceGeneration(),
                    artifact.getSourceChecksum(),
                    artifact.getContentHash(),
                    artifact.getArtifactType(),
                    artifact.getFileType(),
                    artifact.getTitle(),
                    artifact.getVersion(),
                    artifact.getIsCurrent()))
        .toList();
  }

  public List<SourceChunk> chunks(
      String projectId, String sourceObject, String query, Integer requestedLimit) {
    int limit = chunkLimit(requestedLimit);

    Specification<SemanticChunkEntity> spec =
        (root, criteriaQuery, cb) -> cb.equal(root.get("projectId"), projectId);

    if (!isBlank(sourceObject)) {
      spec =
          spec.and((root, criteriaQuery, cb) -> cb.equal(root.get("sourceObject"), sourceObject));
    }

    if (!isBlank(query)) {
      String lowerQuery = "%" + query.toLowerCase() + "%";
      spec =
          spec.and((root, criteriaQuery, cb) -> cb.like(cb.lower(root.get("content")), lowerQuery));
    }

    List<SemanticChunkEntity> chunks =
        semanticChunkRepository
            .findAll(
                spec, PageRequest.of(0, limit, Sort.by("sourceObject").and(Sort.by("chunkIndex"))))
            .getContent();

    return chunks.stream()
        .map(
            chunk ->
                new SourceChunk(
                    chunk.getChunkId(),
                    chunk.getDocumentId(),
                    chunk.getArtifactId(),
                    chunk.getProjectId(),
                    chunk.getSourceBucket(),
                    chunk.getSourceObject(),
                    chunk.getSourceGeneration(),
                    chunk.getSourceChecksum(),
                    chunk.getDocumentContentHash(),
                    chunk.getChunkIndex(),
                    chunk.getTotalChunks(),
                    chunk.getCharStart(),
                    chunk.getCharEnd(),
                    chunk.getChunkContentHash(),
                    chunk.getDomain(),
                    chunk.getSubdomain(),
                    chunk.getContent()))
        .toList();
  }

  public GenericKnowledgeSnapshot knowledge(String projectId, String sourceObject) {
    return new GenericKnowledgeSnapshot(
        projectId,
        sourceObject,
        rows(
            "knowledge.business_rules",
            "k.rule_id, k.project_id, k.rule_name, k.rule_type, k.condition_text, k.outcome_text, "
                + "k.source_business_component_name, k.priority, k.source_artifact_id, k.source_chunk_id, "
                + "k.confidence, k.created_at, k.updated_at",
            projectId,
            sourceObject),
        rows(
            "knowledge.business_flows",
            "k.flow_id, k.project_id, k.flow_name, k.trigger_text, k.outcome_text, k.owner, "
                + "k.source_artifact_id, k.source_chunk_id, k.confidence, k.created_at, k.updated_at",
            projectId,
            sourceObject),
        flowSteps(projectId, sourceObject),
        rows(
            "knowledge.solution_components",
            "k.component_id, k.project_id, k.domain, k.subdomain, k.component_layer, k.component_name, "
                + "k.component_type, k.capability, k.responsibility, k.technology, k.owner, k.lifecycle, "
                + "k.source_artifact_id, k.source_chunk_id, k.confidence, k.created_at, k.updated_at",
            projectId,
            sourceObject),
        rows(
            "knowledge.deployment_resources",
            "k.resource_id, k.project_id, k.domain, k.subdomain, k.resource_name, k.resource_type, k.provider, "
                + "k.hosting_model, k.environment, k.region, k.criticality, k.lifecycle, k.source_artifact_id, "
                + "k.source_chunk_id, k.confidence, k.created_at, k.updated_at",
            projectId,
            sourceObject),
        resourceConfigs(projectId, sourceObject),
        rows(
            "knowledge.knowledge_relationships",
            "k.relationship_id, k.project_id, k.source_name, k.source_type, k.source_ref_id, k.target_name, "
                + "k.target_type, k.target_ref_id, k.relationship_type, k.context, k.source_artifact_id, "
                + "k.source_chunk_id, k.confidence, k.created_at",
            projectId,
            sourceObject),
        rows(
            "knowledge.knowledge_notes",
            "k.note_id, k.project_id, k.note_type, k.note_text, k.source_artifact_id, k.source_chunk_id, k.created_at",
            projectId,
            sourceObject),
        dataModels(projectId, sourceObject),
        dataFields(projectId, sourceObject),
        usageProfiles(projectId, sourceObject),
        rows(
            "knowledge.resource_cost_estimates",
            "k.estimate_id, k.project_id, k.resource_id, k.resource_name, k.environment, k.provider, "
                + "k.billing_model, k.quantity, k.unit, k.unit_cost, k.estimated_monthly_cost, k.currency, "
                + "k.pricing_source, k.pricing_date, k.assumptions, k.source_artifact_id, k.source_chunk_id, "
                + "k.confidence, k.created_at, k.updated_at",
            projectId,
            sourceObject));
  }

  private List<Map<String, Object>> rows(
      String tableName, String columns, String projectId, String sourceObject) {
    String sql =
        "SELECT "
            + columns
            + " FROM "
            + tableName
            + " k "
            + "LEFT JOIN knowledge.artifacts a ON a.artifact_id = k.source_artifact_id "
            + "WHERE k.project_id = :projectId ";
    MapSqlParameterSource params = projectParam(projectId);
    if (!isBlank(sourceObject)) {
      sql += "AND a.source_object = :sourceObject ";
      params.addValue("sourceObject", sourceObject);
    }
    sql += "ORDER BY k.created_at DESC";
    return mapRows(sql, params);
  }

  private List<Map<String, Object>> flowSteps(String projectId, String sourceObject) {
    String sql =
        """
        SELECT s.step_id, s.flow_id, s.sequence_number, s.step_name, s.actor, s.action_text,
          s.input_text, s.output_text, s.next_step, s.created_at
        FROM knowledge.business_flow_steps s
        JOIN knowledge.business_flows f ON f.flow_id = s.flow_id
        LEFT JOIN knowledge.artifacts a ON a.artifact_id = f.source_artifact_id
        WHERE f.project_id = :projectId
        """;
    MapSqlParameterSource params = projectParam(projectId);
    if (!isBlank(sourceObject)) {
      sql += "AND a.source_object = :sourceObject ";
      params.addValue("sourceObject", sourceObject);
    }
    sql += "ORDER BY s.flow_id, s.sequence_number";
    return mapRows(sql, params);
  }

  private List<Map<String, Object>> resourceConfigs(String projectId, String sourceObject) {
    String sql =
        """
        SELECT c.config_id, c.resource_id, c.config_key, c.config_value, c.unit,
          c.source_chunk_id, c.created_at
        FROM knowledge.deployment_resource_configs c
        JOIN knowledge.deployment_resources r ON r.resource_id = c.resource_id
        LEFT JOIN knowledge.artifacts a ON a.artifact_id = r.source_artifact_id
        WHERE r.project_id = :projectId
        """;
    MapSqlParameterSource params = projectParam(projectId);
    if (!isBlank(sourceObject)) {
      sql += "AND a.source_object = :sourceObject ";
      params.addValue("sourceObject", sourceObject);
    }
    sql += "ORDER BY c.resource_id, c.config_key";
    return mapRows(sql, params);
  }

  private List<Map<String, Object>> usageProfiles(String projectId, String sourceObject) {
    String sql =
        """
        SELECT u.profile_id, u.project_id, u.environment, u.users_count, u.requests_per_day,
          u.peak_rps, u.data_ingest_gb_per_day, u.data_retention_days,
          u.storage_growth_gb_per_month, u.availability_target, u.dr_required,
          u.source_chunk_id, u.created_at, u.updated_at
        FROM knowledge.usage_profiles u
        LEFT JOIN knowledge.semantic_chunks c ON c.chunk_id = u.source_chunk_id
        WHERE u.project_id = :projectId
        """;
    MapSqlParameterSource params = projectParam(projectId);
    if (!isBlank(sourceObject)) {
      sql += "AND c.source_object = :sourceObject ";
      params.addValue("sourceObject", sourceObject);
    }
    sql += "ORDER BY u.created_at DESC";
    return mapRows(sql, params);
  }

  private List<Map<String, Object>> dataModels(String projectId, String sourceObject) {
    String sql =
        """
        SELECT dm.data_model_id, dm.artifact_id, dm.project_id, dm.domain_id,
          dm.model_name, dm.model_type, dm.description, dm.schema_definition, dm.created_at
        FROM knowledge.knowledge_data_models dm
        LEFT JOIN knowledge.artifacts a ON a.artifact_id = dm.artifact_id
        WHERE dm.project_id = :projectId
        """;
    MapSqlParameterSource params = projectParam(projectId);
    if (!isBlank(sourceObject)) {
      sql += "AND a.source_object = :sourceObject ";
      params.addValue("sourceObject", sourceObject);
    }
    sql += "ORDER BY dm.created_at DESC";
    return mapRows(sql, params);
  }

  private List<Map<String, Object>> dataFields(String projectId, String sourceObject) {
    String sql =
        """
        SELECT df.field_id, df.data_model_id, df.field_name, df.field_type,
          df.is_required, df.description, df.constraints, df.created_at
        FROM knowledge.knowledge_data_fields df
        JOIN knowledge.knowledge_data_models dm ON dm.data_model_id = df.data_model_id
        LEFT JOIN knowledge.artifacts a ON a.artifact_id = dm.artifact_id
        WHERE dm.project_id = :projectId
        """;
    MapSqlParameterSource params = projectParam(projectId);
    if (!isBlank(sourceObject)) {
      sql += "AND a.source_object = :sourceObject ";
      params.addValue("sourceObject", sourceObject);
    }
    sql += "ORDER BY df.data_model_id, df.created_at";
    return mapRows(sql, params);
  }

  private List<Map<String, Object>> mapRows(String sql, MapSqlParameterSource params) {
    return jdbc.query(
        sql,
        params,
        resultSet -> {
          List<Map<String, Object>> rows = new ArrayList<>();
          ResultSetMetaData metadata = resultSet.getMetaData();
          while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
              row.put(metadata.getColumnLabel(i), resultSet.getObject(i));
            }
            rows.add(row);
          }
          return rows;
        });
  }

  private MapSqlParameterSource projectParam(String projectId) {
    return new MapSqlParameterSource().addValue("projectId", projectId);
  }

  private int chunkLimit(Integer requestedLimit) {
    if (requestedLimit == null || requestedLimit <= 0) {
      return DEFAULT_CHUNK_LIMIT;
    }
    return Math.min(requestedLimit, MAX_CHUNK_LIMIT);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
