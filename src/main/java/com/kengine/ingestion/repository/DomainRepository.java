package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.DomainEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainRepository extends JpaRepository<DomainEntity, UUID> {
  Optional<DomainEntity> findBySubjectIdAndDomain(UUID subjectId, String domainName);

  int deleteBySubjectId(UUID subjectId);

  long countBySubjectId(UUID subjectId);
}
