package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.DocumentKnowledgeEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentKnowledgeRepository extends JpaRepository<DocumentKnowledgeEntity, UUID> {
  int deleteBySubjectId(UUID subjectId);

  long countBySubjectId(UUID subjectId);
}
