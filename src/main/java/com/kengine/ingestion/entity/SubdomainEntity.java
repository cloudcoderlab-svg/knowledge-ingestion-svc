package com.kengine.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "subdomains")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubdomainEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "subdomain_id", nullable = false)
  private UUID subdomainId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "domain_id", nullable = false)
  private UUID domainId;

  @Column(name = "subdomain", nullable = false, length = 500)
  private String subdomain;

  @Column(name = "description", columnDefinition = "text")
  private String description;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
