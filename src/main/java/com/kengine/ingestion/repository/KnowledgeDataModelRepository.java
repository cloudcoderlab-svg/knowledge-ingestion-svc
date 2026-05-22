package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeDataModelEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeDataModelRepository
    extends JpaRepository<KnowledgeDataModelEntity, UUID> {}
