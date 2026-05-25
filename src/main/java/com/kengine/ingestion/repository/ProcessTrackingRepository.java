package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.ProcessTrackingEntity;
import com.kengine.ingestion.model.ProcessStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessTrackingRepository extends JpaRepository<ProcessTrackingEntity, UUID> {

  List<ProcessTrackingEntity> findBySubjectIdOrderByCreatedAtDesc(UUID subjectId);

  List<ProcessTrackingEntity> findByStatusOrderByCreatedAtDesc(ProcessStatus status);

  List<ProcessTrackingEntity> findBySubjectIdAndStatusOrderByCreatedAtDesc(
      UUID subjectId, ProcessStatus status);
}
