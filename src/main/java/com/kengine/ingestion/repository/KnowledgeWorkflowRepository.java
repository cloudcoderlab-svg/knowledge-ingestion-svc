package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeWorkflowEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeWorkflowRepository extends JpaRepository<KnowledgeWorkflowEntity, UUID> {}
