package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeIntegrationEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeIntegrationRepository
    extends JpaRepository<KnowledgeIntegrationEntity, UUID> {
  int deleteByArtifactId(UUID artifactId);
}
