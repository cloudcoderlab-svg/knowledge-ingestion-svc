package com.kengine.knowledge.repository;

import com.kengine.knowledge.entity.ProjectEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {
  Optional<ProjectEntity> findByProjectNameAndVersion(String projectName, Integer version);

  Optional<ProjectEntity> findByGcsPrefix(String gcsPrefix);

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
}
