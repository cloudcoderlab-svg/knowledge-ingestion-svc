package com.kengine.ingestion.dto;

import java.util.List;
import lombok.Data;

@Data
public class KnowledgeExtractionResult {
  private String architecturalSummary;
  private List<BusinessRule> businessRules;
  private List<BusinessFlow> businessFlows;
  private List<BusinessComponent> businessComponents;
  private List<TechnicalComponent> technicalComponents;
  private List<DeploymentResource> deploymentResources;
  private List<KnowledgeRelationship> relationships;
  private List<UsageProfile> usageProfiles;
  private List<ResourceCostEstimate> costEstimates;
  private List<String> migrationNotes;
}
