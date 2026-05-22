package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.ProjectEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
  Optional<ProjectEntity> findByProjectId(String projectId);
}
