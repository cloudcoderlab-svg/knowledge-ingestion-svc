package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.DomainEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing domain entities.
 *
 * <p>Domains represent high-level business areas or functional domains within a subject's knowledge
 * graph (e.g., "Order Management", "User Authentication", "Payment Processing").
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on domains
 *   <li>Lookup by subject and domain name
 *   <li>Bulk deletion by subject
 * </ul>
 */
@Repository
public interface DomainRepository extends JpaRepository<DomainEntity, UUID> {
  /**
   * Finds a domain by subject ID and domain name.
   *
   * @param subjectId Subject UUID
   * @param domainName Domain name (e.g., "Order Management")
   * @return Optional containing the domain if found
   */
  Optional<DomainEntity> findBySubjectIdAndDomain(UUID subjectId, String domainName);

  /**
   * Deletes all domains belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of domains deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Counts total domains for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of domains
   */
  long countBySubjectId(UUID subjectId);
}
