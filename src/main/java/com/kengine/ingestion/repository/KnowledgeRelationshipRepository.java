package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.KnowledgeRelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeRelationshipRepository
    extends JpaRepository<KnowledgeRelationshipEntity, String> {}
