package com.kengine.ingestion.dto;

import lombok.Data;

@Data
public class UsageProfile {
  private String environment;
  private Long usersCount;
  private Long requestsPerDay;
  private Double peakRps;
  private Double dataIngestGbPerDay;
  private Long dataRetentionDays;
  private Double storageGrowthGbPerMonth;
  private String availabilityTarget;
  private Boolean drRequired;
}
