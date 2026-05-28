package com.kengine.knowledge.repository;

import com.kengine.knowledge.entity.DataFieldEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataFieldRepository extends JpaRepository<DataFieldEntity, UUID> {}
