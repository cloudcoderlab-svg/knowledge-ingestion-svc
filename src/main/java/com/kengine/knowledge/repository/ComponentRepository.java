package com.kengine.knowledge.repository;

import com.kengine.knowledge.entity.ComponentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponentRepository extends JpaRepository<ComponentEntity, UUID> {
  List<ComponentEntity> findByProjectId(UUID projectId);
}
