package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeResourceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeResourceRepository extends JpaRepository<KnowledgeResourceEntity, UUID> {
  int deleteBySubjectId(UUID subjectId);

  int deleteByArtifactId(UUID artifactId);

  long countBySubjectId(UUID subjectId);
}
