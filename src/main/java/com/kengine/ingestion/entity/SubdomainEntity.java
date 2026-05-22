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

  @Column(name = "domain_id")
  private UUID domainId;

  @Column(name = "project_id", nullable = false)
  private String projectId;

  @Column(name = "domain", nullable = false)
  private String domain;

  @Column(name = "subdomain", nullable = false)
  private String subdomain;

  @Column(name = "created_at")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
