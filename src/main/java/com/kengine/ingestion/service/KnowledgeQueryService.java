package com.kengine.ingestion.service;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Type.Code;
import com.kengine.ingestion.dto.GenericKnowledgeSnapshot;
import com.kengine.ingestion.dto.KnowledgeSource;
import com.kengine.ingestion.dto.SourceChunk;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeQueryService {

  private static final int DEFAULT_CHUNK_LIMIT = 100;
  private static final int MAX_CHUNK_LIMIT = 1000;

  private final DatabaseClient databaseClient;

  public List<KnowledgeSource> sources(String projectId) {
    Statement statement =
        Statement.newBuilder(
                "SELECT artifact_id, project_id, domain, subdomain, source_bucket, source_object, "
                    + "source_generation, source_checksum, content_hash, artifact_type, file_type, "
                    + "title, version, is_current "
                    + "FROM knowledge.artifacts "
                    + "WHERE project_id = $1 "
                    + "ORDER BY source_object, created_at DESC")
            .bind("p1")
            .to(projectId)
            .build();

    List<KnowledgeSource> sources = new ArrayList<>();
    try (ResultSet rows = databaseClient.singleUse().executeQuery(statement)) {
      while (rows.next()) {
        sources.add(
            new KnowledgeSource(
                string(rows, "artifact_id"),
                string(rows, "project_id"),
                string(rows, "domain"),
                string(rows, "subdomain"),
                string(rows, "source_bucket"),
                string(rows, "source_object"),
                longValue(rows, "source_generation"),
                string(rows, "source_checksum"),
                string(rows, "content_hash"),
                string(rows, "artifact_type"),
                string(rows, "file_type"),
                string(rows, "title"),
                string(rows, "version"),
                bool(rows, "is_current")));
      }
    }
    return sources;
  }

  public List<SourceChunk> chunks(
      String projectId, String sourceObject, String query, Integer requestedLimit) {
    int limit = chunkLimit(requestedLimit);
    int parameterIndex = 1;
    StringBuilder sql =
        new StringBuilder(
            "SELECT chunk_id, document_id, artifact_id, project_id, source_bucket, source_object, "
                + "source_generation, source_checksum, document_content_hash, chunk_index, "
                + "total_chunks, char_start, char_end, chunk_content_hash, domain, subdomain, content "
                + "FROM knowledge.semantic_chunks "
                + "WHERE project_id = $"
                + parameterIndex
                + " ");
    if (!isBlank(sourceObject)) {
      sql.append("AND source_object = $").append(++parameterIndex).append(" ");
    }
    if (!isBlank(query)) {
      sql.append("AND LOWER(content) LIKE $").append(++parameterIndex).append(" ");
    }
    sql.append("ORDER BY source_object, chunk_index LIMIT $").append(++parameterIndex);

    Statement.Builder builder = Statement.newBuilder(sql.toString()).bind("p1").to(projectId);
    int bindIndex = 1;
    if (!isBlank(sourceObject)) {
      builder.bind("p" + ++bindIndex).to(sourceObject);
    }
    if (!isBlank(query)) {
      builder.bind("p" + ++bindIndex).to("%" + query.toLowerCase(Locale.ROOT) + "%");
    }
    builder.bind("p" + ++bindIndex).to((long) limit);

    List<SourceChunk> chunks = new ArrayList<>();
    try (ResultSet rows = databaseClient.singleUse().executeQuery(builder.build())) {
      while (rows.next()) {
        chunks.add(
            new SourceChunk(
                string(rows, "chunk_id"),
                string(rows, "document_id"),
                string(rows, "artifact_id"),
                string(rows, "project_id"),
                string(rows, "source_bucket"),
                string(rows, "source_object"),
                longValue(rows, "source_generation"),
                string(rows, "source_checksum"),
                string(rows, "document_content_hash"),
                longValue(rows, "chunk_index"),
                longValue(rows, "total_chunks"),
                longValue(rows, "char_start"),
                longValue(rows, "char_end"),
                string(rows, "chunk_content_hash"),
                string(rows, "domain"),
                string(rows, "subdomain"),
                string(rows, "content")));
      }
    }
    return chunks;
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
            + "WHERE k.project_id = $1 ";
    if (!isBlank(sourceObject)) {
      sql += "AND a.source_object = $2 ";
    }
    sql += "ORDER BY k.created_at DESC";

    Statement.Builder builder = Statement.newBuilder(sql).bind("p1").to(projectId);
    if (!isBlank(sourceObject)) {
      builder.bind("p2").to(sourceObject);
    }
    return mapRows(builder.build());
  }

  private List<Map<String, Object>> flowSteps(String projectId, String sourceObject) {
    String sql =
        "SELECT s.step_id, s.flow_id, s.sequence_number, s.step_name, s.actor, s.action_text, "
            + "s.input_text, s.output_text, s.next_step, s.created_at "
            + "FROM knowledge.business_flow_steps s "
            + "JOIN knowledge.business_flows f ON f.flow_id = s.flow_id "
            + "LEFT JOIN knowledge.artifacts a ON a.artifact_id = f.source_artifact_id "
            + "WHERE f.project_id = $1 ";
    if (!isBlank(sourceObject)) {
      sql += "AND a.source_object = $2 ";
    }
    sql += "ORDER BY s.flow_id, s.sequence_number";
    Statement.Builder builder = Statement.newBuilder(sql).bind("p1").to(projectId);
    if (!isBlank(sourceObject)) {
      builder.bind("p2").to(sourceObject);
    }
    return mapRows(builder.build());
  }

  private List<Map<String, Object>> resourceConfigs(String projectId, String sourceObject) {
    String sql =
        "SELECT c.config_id, c.resource_id, c.config_key, c.config_value, c.unit, "
            + "c.source_chunk_id, c.created_at "
            + "FROM knowledge.deployment_resource_configs c "
            + "JOIN knowledge.deployment_resources r ON r.resource_id = c.resource_id "
            + "LEFT JOIN knowledge.artifacts a ON a.artifact_id = r.source_artifact_id "
            + "WHERE r.project_id = $1 ";
    if (!isBlank(sourceObject)) {
      sql += "AND a.source_object = $2 ";
    }
    sql += "ORDER BY c.resource_id, c.config_key";
    Statement.Builder builder = Statement.newBuilder(sql).bind("p1").to(projectId);
    if (!isBlank(sourceObject)) {
      builder.bind("p2").to(sourceObject);
    }
    return mapRows(builder.build());
  }

  private List<Map<String, Object>> usageProfiles(String projectId, String sourceObject) {
    String sql =
        "SELECT u.profile_id, u.project_id, u.environment, u.users_count, u.requests_per_day, "
            + "u.peak_rps, u.data_ingest_gb_per_day, u.data_retention_days, "
            + "u.storage_growth_gb_per_month, u.availability_target, u.dr_required, "
            + "u.source_chunk_id, u.created_at, u.updated_at "
            + "FROM knowledge.usage_profiles u "
            + "LEFT JOIN knowledge.semantic_chunks c ON c.chunk_id = u.source_chunk_id "
            + "WHERE u.project_id = $1 ";
    if (!isBlank(sourceObject)) {
      sql += "AND c.source_object = $2 ";
    }
    sql += "ORDER BY u.created_at DESC";
    Statement.Builder builder = Statement.newBuilder(sql).bind("p1").to(projectId);
    if (!isBlank(sourceObject)) {
      builder.bind("p2").to(sourceObject);
    }
    return mapRows(builder.build());
  }

  private List<Map<String, Object>> mapRows(Statement statement) {
    List<Map<String, Object>> rows = new ArrayList<>();
    try (ResultSet resultSet = databaseClient.singleUse().executeQuery(statement)) {
      while (resultSet.next()) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < resultSet.getColumnCount(); i++) {
          String columnName = resultSet.getMetadata().getRowType().getFields(i).getName();
          row.put(columnName, value(resultSet, i));
        }
        rows.add(row);
      }
    }
    return rows;
  }

  private Object value(ResultSet rows, int index) {
    if (rows.isNull(index)) {
      return null;
    }
    Code code = rows.getColumnType(index).getCode();
    return switch (code) {
      case BOOL -> rows.getBoolean(index);
      case INT64 -> rows.getLong(index);
      case FLOAT64 -> rows.getDouble(index);
      case DATE -> rows.getDate(index).toString();
      case TIMESTAMP -> rows.getTimestamp(index).toString();
      default -> rows.getString(index);
    };
  }

  private int chunkLimit(Integer requestedLimit) {
    if (requestedLimit == null || requestedLimit <= 0) {
      return DEFAULT_CHUNK_LIMIT;
    }
    return Math.min(requestedLimit, MAX_CHUNK_LIMIT);
  }

  private String string(ResultSet rows, String columnName) {
    return rows.isNull(columnName) ? null : rows.getString(columnName);
  }

  private Long longValue(ResultSet rows, String columnName) {
    return rows.isNull(columnName) ? null : rows.getLong(columnName);
  }

  private Boolean bool(ResultSet rows, String columnName) {
    return rows.isNull(columnName) ? null : rows.getBoolean(columnName);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
