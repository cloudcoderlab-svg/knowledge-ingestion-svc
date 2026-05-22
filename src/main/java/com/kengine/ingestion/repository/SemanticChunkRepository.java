package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.SemanticChunkEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SemanticChunkRepository
    extends JpaRepository<SemanticChunkEntity, String>,
        JpaSpecificationExecutor<SemanticChunkEntity> {

  List<SemanticChunkEntity> findByProjectIdAndSourceObjectOrderByChunkIndex(
      String projectId, String sourceObject);
}
