package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeAPIEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeAPIRepository extends JpaRepository<KnowledgeAPIEntity, UUID> {
  int deleteBySubjectId(UUID subjectId);

  int deleteByArtifactId(UUID artifactId);

  long countBySubjectId(UUID subjectId);
}
