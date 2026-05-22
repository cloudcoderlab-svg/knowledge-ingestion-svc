package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeComponentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeComponentRepository
    extends JpaRepository<KnowledgeComponentEntity, UUID> {
  List<KnowledgeComponentEntity> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
