package com.kengine.ingestion.repository;

import com.kengine.ingestion.entity.SubdomainEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing subdomain entities.
 *
 * <p>Subdomains represent finer-grained business areas within a domain, creating a hierarchical
 * organization of knowledge (e.g., "Payment Processing" domain might have "Credit Card" and
 * "PayPal" subdomains).
 *
 * <p>Key capabilities:
 *
 * <ul>
 *   <li>CRUD operations on subdomains
 *   <li>Lookup by domain and subdomain name
 *   <li>Bulk deletion by subject
 * </ul>
 */
@Repository
public interface SubdomainRepository extends JpaRepository<SubdomainEntity, UUID> {
  /**
   * Finds a subdomain by domain ID and subdomain name.
   *
   * @param domainId Domain UUID
   * @param subdomainName Subdomain name (e.g., "Credit Card")
   * @return Optional containing the subdomain if found
   */
  Optional<SubdomainEntity> findByDomainIdAndSubdomain(UUID domainId, String subdomainName);

  /**
   * Deletes all subdomains belonging to a subject.
   *
   * @param subjectId Subject UUID
   * @return Number of subdomains deleted
   */
  int deleteBySubjectId(UUID subjectId);

  /**
   * Counts total subdomains for a subject.
   *
   * @param subjectId Subject UUID
   * @return Count of subdomains
   */
  long countBySubjectId(UUID subjectId);
}
