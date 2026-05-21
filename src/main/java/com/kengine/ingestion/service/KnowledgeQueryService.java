package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.GenericKnowledgeSnapshot;
import com.kengine.ingestion.dto.KnowledgeSource;
import com.kengine.ingestion.dto.SourceChunk;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryService {

  private static final int DEFAULT_CHUNK_LIMIT = 100;
  private static final int MAX_CHUNK_LIMIT = 1000;

  private final NamedParameterJdbcTemplate jdbc;

  public List<KnowledgeSource> sources(String projectId) {
    return jdbc.query(
        """
        SELECT artifact_id, project_id, domain, subdomain, source_bucket, source_object,
          source_generation, source_checksum, content_hash, artifact_type, file_type,
          title, version, is_current
        FROM knowledge.artifacts
        WHERE project_id = :projectId
        ORDER BY source_object, created_at DESC
        """,
        projectParam(projectId),
        (rows, ignored) ->
            new KnowledgeSource(
                rows.getString("artifact_id"),
                rows.getString("project_id"),
                rows.getString("domain"),
                rows.getString("subdomain"),
                rows.getString("source_bucket"),
                rows.getString("source_object"),
                longValue(rows, "source_generation"),
                rows.getString("source_checksum"),
                rows.getString("content_hash"),
                rows.getString("artifact_type"),
                rows.getString("file_type"),
                rows.getString("title"),
                rows.getString("version"),
                bool(rows, "is_current")));
  }

  public List<SourceChunk> chunks(
      String projectId, String sourceObject, String query, Integer requestedLimit) {
    int limit = chunkLimit(requestedLimit);
    StringBuilder sql =
        new StringBuilder(
            """
            SELECT chunk_id, document_id, artifact_id, project_id, source_bucket, source_object,
              source_generation, source_checksum, document_content_hash, chunk_index,
              total_chunks, char_start, char_end, chunk_content_hash, domain, subdomain, content
            FROM knowledge.semantic_chunks
            WHERE project_id = :projectId
            """);
    MapSqlParameterSource params = projectParam(projectId).addValue("limit", limit);
    if (!isBlank(sourceObject)) {
      sql.append("AND source_object = :sourceObject ");
      params.addValue("sourceObject", sourceObject);
    }
    if (!isBlank(query)) {
      sql.append("AND LOWER(content) LIKE :query ");
      params.addValue("query", "%" + query.toLowerCase(Locale.ROOT) + "%");
    }
    sql.append("ORDER BY source_object, chunk_index LIMIT :limit");

    return jdbc.query(
        sql.toString(),
        params,
        (rows, ignored) ->
            new SourceChunk(
                rows.getString("chunk_id"),
                rows.getString("document_id"),
                rows.getString("artifact_id"),
                rows.getString("project_id"),
                rows.getString("source_bucket"),
                rows.getString("source_object"),
                longValue(rows, "source_generation"),
                rows.getString("source_checksum"),
                rows.getString("document_content_hash"),
                longValue(rows, "chunk_index"),
                longValue(rows, "total_chunks"),
                longValue(rows, "char_start"),
                longValue(rows, "char_end"),
                rows.getString("chunk_content_hash"),
                rows.getString("domain"),
                rows.getString("subdomain"),
                rows.getString("content")));
  }

  public GenericKnowledgeSnapshot knowledge(String projectId, String sourceObject) {
    return new GenericKnowledgeSnapshot(
        projectId,
        sourceObject,
        rows(
            "knowledge.business_rules",
            "rule_id, project_id, rule_name, rule_type, condition_text, outcome_text, "
                + "source_business_component_name, priority, source_artifact_id, source_chunk_id, "
                + "confidence, created_at, updated_at",
            projectId,
            sourceObject),
        rows(
            "knowledge.business_flows",
            "flow_id, project_id, flow_name, trigger_text, outcome_text, owner, "
                + "source_artifact_id, source_chunk_id, confidence, created_at, updated_at",
            projectId,
            sourceObject),
        flowSteps(projectId, sourceObject),
        rows(
            "knowledge.solution_components",
            "component_id, project_id, domain, subdomain, component_layer, component_name, "
                + "component_type, capability, responsibility, technology, owner, lifecycle, "
                + "source_artifact_id, source_chunk_id, confidence, created_at, updated_at",
            projectId,
            sourceObject),
        rows(
            "knowledge.deployment_resources",
            "resource_id, project_id, domain, subdomain, resource_name, resource_type, provider, "
                + "hosting_model, environment, region, criticality, lifecycle, source_artifact_id, "
                + "source_chunk_id, confidence, created_at, updated_at",
            projectId,
            sourceObject),
        resourceConfigs(projectId, sourceObject),
        rows(
            "knowledge.knowledge_relationships",
            "relationship_id, project_id, source_name, source_type, source_ref_id, target_name, "
                + "target_type, target_ref_id, relationship_type, context, source_artifact_id, "
                + "source_chunk_id, confidence, created_at",
            projectId,
            sourceObject),
        rows(
            "knowledge.knowledge_notes",
            "note_id, project_id, note_type, note_text, source_artifact_id, source_chunk_id, created_at",
            projectId,
            sourceObject),
        usageProfiles(projectId, sourceObject),
        rows(
            "knowledge.resource_cost_estimates",
            "estimate_id, project_id, resource_id, resource_name, environment, provider, "
                + "billing_model, quantity, unit, unit_cost, estimated_monthly_cost, currency, "
                + "pricing_source, pricing_date, assumptions, source_artifact_id, source_chunk_id, "
                + "confidence, created_at, updated_at",
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

  private Long longValue(ResultSet rows, String columnName) throws SQLException {
    long value = rows.getLong(columnName);
    return rows.wasNull() ? null : value;
  }

  private Boolean bool(ResultSet rows, String columnName) throws SQLException {
    boolean value = rows.getBoolean(columnName);
    return rows.wasNull() ? null : value;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
