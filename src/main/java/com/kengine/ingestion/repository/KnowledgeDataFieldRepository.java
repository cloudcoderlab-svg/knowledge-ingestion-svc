package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeDataFieldEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeDataFieldRepository
    extends JpaRepository<KnowledgeDataFieldEntity, UUID> {}
