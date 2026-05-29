package com.kengine.knowledge.repository;

import com.kengine.knowledge.entity.ProjectEntity;
import com.kengine.knowledge.entity.ProjectStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
  Optional<ProjectEntity> findByProjectNameAndVersion(String projectName, Integer version);

  @Query(
      "SELECT p FROM ProjectEntity p WHERE p.gcsPrefix = :gcsPrefix "
          + "ORDER BY p.version DESC, p.createdAt DESC LIMIT 1")
  Optional<ProjectEntity> findByGcsPrefix(@Param("gcsPrefix") String gcsPrefix);

  @Query(
      value =
          "select "
              + "project_id as projectId, "
              + "project_name as projectName, "
              + "version, title, description, definition, "
              + "source_bucket as sourceBucket, "
              + "gcs_prefix as gcsPrefix, "
              + "metadata::text as metadata, "
              + "(1 / (1 + (definition_embedding <=> cast(:embedding as vector)))) as score "
              + "from knowledge.projects "
              + "where definition_embedding is not null "
              + "order by definition_embedding <=> cast(:embedding as vector) limit :limit",
      nativeQuery = true)
  List<ProjectDiscoveryProjection> searchByDefinitionEmbedding(
      @Param("embedding") String embedding, @Param("limit") int limit);

  List<ProjectEntity> findByProjectNameOrderByVersionDesc(String projectName);

  @Query(
      "SELECT COALESCE(MAX(p.version), 0) FROM ProjectEntity p WHERE p.projectName = :projectName")
  Integer findMaxVersionByProjectName(@Param("projectName") String projectName);

  List<ProjectEntity> findByProjectNameAndStatusNot(String projectName, ProjectStatus status);
}
