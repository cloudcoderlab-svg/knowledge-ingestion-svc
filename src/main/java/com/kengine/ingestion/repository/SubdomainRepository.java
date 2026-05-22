package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.SubdomainEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubdomainRepository extends JpaRepository<SubdomainEntity, UUID> {
  Optional<SubdomainEntity> findByDomainIdAndSubdomain(UUID domainId, String subdomainName);
}
