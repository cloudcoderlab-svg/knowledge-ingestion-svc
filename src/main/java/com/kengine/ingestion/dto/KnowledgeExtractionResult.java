package com.kengine.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowledgeExtractionResult {
  private String architecturalSummary;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessRule> businessRules;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessFlow> businessFlows;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<BusinessComponent> businessComponents;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<TechnicalComponent> technicalComponents;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<DeploymentResource> deploymentResources;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<KnowledgeRelationship> relationships;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<UsageProfile> usageProfiles;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<ResourceCostEstimate> costEstimates;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<String> migrationNotes;
}
