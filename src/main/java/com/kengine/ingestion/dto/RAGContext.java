package com.kengine.ingestion.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RAGContext {
  private String query;
  private List<KnowledgeComponent> components;
  private List<KnowledgeAPI> apis;
  private List<KnowledgeBusinessRule> businessRules;
  private List<KnowledgeWorkflow> workflows;
  private List<KnowledgeDataModel> dataModels;
  private List<KnowledgeIntegration> integrations;
  private List<KnowledgeResource> resources;
  private List<KnowledgeRelationship> relationships;
  private Map<String, Double> relevanceScores;
}
