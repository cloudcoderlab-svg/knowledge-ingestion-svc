package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.SubjectEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, UUID> {

  Optional<SubjectEntity> findBySubjectName(String subjectName);

  boolean existsBySubjectName(String subjectName);
}
