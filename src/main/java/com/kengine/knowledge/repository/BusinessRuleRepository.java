package com.kengine.knowledge.repository;

import com.kengine.knowledge.entity.BusinessRuleEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRuleRepository extends JpaRepository<BusinessRuleEntity, UUID> {
  List<BusinessRuleEntity> findByProjectId(UUID projectId);
}
