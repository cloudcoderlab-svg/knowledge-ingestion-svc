package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service responsible for building and persisting the knowledge graph. Coordinates extraction of
 * entities from document and chunk knowledge, generates embeddings, and persists to dedicated
 * knowledge tables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGraphService {

  private final KnowledgeEntityEmbeddingService embeddingService;
  private final PostgresStorageService storageService;

  @Value("${ingestion.knowledge-graph.enable-persistence:true}")
  private boolean enablePersistence;

  @Value("${ingestion.knowledge-graph.batch-size:100}")
  private int batchSize;

  /**
   * Builds the knowledge graph from document-level and chunk-level knowledge. Extracts entities,
   * generates embeddings, and persists to database.
   *
   * @param artifactId the UUID of the artifact in the database
   * @param source source document metadata
   * @param docKnowledge document-level knowledge
   * @param chunkKnowledge list of chunk-level knowledge extractions
   */
  public void buildKnowledgeGraph(
      String artifactId,
      SourceDocumentMetadata source,
      DocumentKnowledge docKnowledge,
      List<KnowledgeExtractionResult> chunkKnowledge) {

    if (!enablePersistence) {
      log.info(
          "Knowledge graph persistence is disabled. Skipping for artifact: {}",
          source.objectName());
      return;
    }

    try {
      log.info("Building knowledge graph for artifact: {}", source.objectName());

      // 1. Ensure domain and subdomain exist and get their UUIDs
      UUID domainId = null;
      UUID subdomainId = null;

      if (docKnowledge != null && docKnowledge.getDomain() != null) {
        domainId = ensureDomain(source.projectId(), docKnowledge.getDomain());

        if (docKnowledge.getSubdomain() != null) {
          subdomainId =
              ensureSubdomain(
                  source.projectId(),
                  docKnowledge.getDomain(),
                  domainId,
                  docKnowledge.getSubdomain());
        }
      }

      // 2. Save document-level knowledge
      if (docKnowledge != null) {
        saveDocumentKnowledge(artifactId, docKnowledge);
      }

      // 3. Extract and save components from both document and chunks
      List<KnowledgeComponent> allComponents =
          extractComponents(
              artifactId, source, docKnowledge, chunkKnowledge, domainId, subdomainId);
      Map<String, UUID> componentNameToId = saveComponents(allComponents);

      // 4. Extract and save business rules
      List<KnowledgeBusinessRule> allRules =
          extractBusinessRules(artifactId, source, chunkKnowledge, domainId, componentNameToId);
      saveBusinessRules(allRules);

      // 5. Extract and save workflows
      List<KnowledgeWorkflow> allWorkflows =
          extractWorkflows(artifactId, source, chunkKnowledge, domainId);
      saveWorkflows(allWorkflows);

      // 7. Extract and save resources
      List<KnowledgeResource> allResources =
          extractResources(artifactId, source, chunkKnowledge, componentNameToId);
      saveResources(allResources);

      // 8. Extract and save relationships
      List<KnowledgeRelationship> allRelationships =
          extractRelationships(artifactId, source, chunkKnowledge);
      saveRelationships(artifactId, source, allRelationships);

      log.info(
          "Knowledge graph built successfully for artifact: {}. "
              + "Components: {}, Rules: {}, Workflows: {}, Resources: {}",
          source.objectName(),
          allComponents.size(),
          allRules.size(),
          allWorkflows.size(),
          allResources.size());

    } catch (Exception e) {
      log.error("Error building knowledge graph for artifact: {}", source.objectName(), e);
      throw new RuntimeException("Failed to build knowledge graph", e);
    }
  }

  /**
   * Ensures a domain exists and returns its UUID. Creates if doesn't exist, returns existing UUID
   * if it does.
   */
  public UUID ensureDomain(String projectId, String domainName) {
    try {
      return storageService.ensureDomain(projectId, domainName);
    } catch (Exception e) {
      log.error("Failed to ensure domain: {}", domainName, e);
      throw new RuntimeException("Failed to ensure domain", e);
    }
  }

  /** Ensures a subdomain exists under a domain and returns its UUID. */
  public UUID ensureSubdomain(
      String projectId, String domainName, UUID domainId, String subdomainName) {
    try {
      return storageService.ensureSubdomain(projectId, domainName, domainId, subdomainName);
    } catch (Exception e) {
      log.error("Failed to ensure subdomain: {}", subdomainName, e);
      throw new RuntimeException("Failed to ensure subdomain", e);
    }
  }

  private void saveDocumentKnowledge(String artifactId, DocumentKnowledge docKnowledge) {
    try {
      storageService.saveDocumentKnowledge(artifactId, docKnowledge);
      log.debug("Saved document knowledge for artifact: {}", artifactId);
    } catch (Exception e) {
      log.error("Failed to save document knowledge", e);
    }
  }

  private List<KnowledgeComponent> extractComponents(
      String artifactId,
      SourceDocumentMetadata source,
      DocumentKnowledge docKnowledge,
      List<KnowledgeExtractionResult> chunkKnowledge,
      UUID domainId,
      UUID subdomainId) {

    List<KnowledgeComponent> components = new ArrayList<>();

    // Extract from chunk knowledge (both business and technical components)
    for (KnowledgeExtractionResult chunkResult : chunkKnowledge) {
      // Business components
      if (chunkResult.getBusinessComponents() != null) {
        chunkResult
            .getBusinessComponents()
            .forEach(
                bc -> {
                  KnowledgeComponent component =
                      KnowledgeComponent.builder()
                          .artifactId(artifactId)
                          .projectId(source.projectId())
                          .domainId(domainId)
                          .subdomainId(subdomainId)
                          .componentName(bc.getComponentName())
                          .componentType(bc.getComponentType())
                          .category("business")
                          .capability(bc.getCapability())
                          .owner(bc.getOwner())
                          .lifecycle(bc.getLifecycle())
                          .confidence(bc.getConfidence())
                          .build();

                  // Generate embedding
                  component.setEmbedding(embeddingService.embedComponent(component));
                  components.add(component);
                });
      }

      // Technical components
      if (chunkResult.getTechnicalComponents() != null) {
        chunkResult
            .getTechnicalComponents()
            .forEach(
                tc -> {
                  KnowledgeComponent component =
                      KnowledgeComponent.builder()
                          .artifactId(artifactId)
                          .projectId(source.projectId())
                          .domainId(domainId)
                          .subdomainId(subdomainId)
                          .componentName(tc.getComponentName())
                          .componentType(tc.getComponentType())
                          .category("technical")
                          .responsibility(tc.getResponsibility())
                          .technology(tc.getTechnology())
                          .lifecycle(tc.getLifecycle())
                          .confidence(tc.getConfidence())
                          .build();

                  // Generate embedding
                  component.setEmbedding(embeddingService.embedComponent(component));
                  components.add(component);
                });
      }
    }

    return components;
  }

  private Map<String, UUID> saveComponents(List<KnowledgeComponent> components) {
    Map<String, UUID> nameToId = new HashMap<>();

    for (KnowledgeComponent component : components) {
      try {
        UUID componentId = storageService.saveKnowledgeComponent(component);
        nameToId.put(component.getComponentName(), componentId);
      } catch (Exception e) {
        log.error("Failed to save component: {}", component.getComponentName(), e);
      }
    }

    log.info("Saved {} components", components.size());
    return nameToId;
  }

  private List<KnowledgeBusinessRule> extractBusinessRules(
      String artifactId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunkKnowledge,
      UUID domainId,
      Map<String, UUID> componentNameToId) {

    List<KnowledgeBusinessRule> rules = new ArrayList<>();

    for (KnowledgeExtractionResult chunkResult : chunkKnowledge) {
      if (chunkResult.getBusinessRules() != null) {
        chunkResult
            .getBusinessRules()
            .forEach(
                br -> {
                  UUID componentId = componentNameToId.get(br.getSourceBusinessComponentName());

                  KnowledgeBusinessRule rule =
                      KnowledgeBusinessRule.builder()
                          .artifactId(artifactId)
                          .componentId(componentId)
                          .projectId(source.projectId())
                          .domainId(domainId)
                          .ruleName(br.getRuleName())
                          .ruleType(br.getRuleType())
                          .conditionText(br.getCondition())
                          .outcomeText(br.getOutcome())
                          .priority(br.getPriority())
                          .confidence(br.getConfidence())
                          .build();

                  rules.add(rule);
                });
      }
    }

    return rules;
  }

  private void saveBusinessRules(List<KnowledgeBusinessRule> rules) {
    for (KnowledgeBusinessRule rule : rules) {
      try {
        rule.setEmbedding(embeddingService.embedBusinessRule(rule));
        storageService.saveKnowledgeBusinessRule(rule);
      } catch (Exception e) {
        log.error("Failed to save business rule: {}", rule.getRuleName(), e);
      }
    }
    log.info("Saved {} business rules", rules.size());
  }

  private List<KnowledgeWorkflow> extractWorkflows(
      String artifactId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunkKnowledge,
      UUID domainId) {

    List<KnowledgeWorkflow> workflows = new ArrayList<>();

    for (KnowledgeExtractionResult chunkResult : chunkKnowledge) {
      if (chunkResult.getBusinessFlows() != null) {
        chunkResult
            .getBusinessFlows()
            .forEach(
                bf -> {
                  // Convert BusinessFlowSteps to KnowledgeWorkflowSteps
                  List<KnowledgeWorkflowStep> steps = new ArrayList<>();
                  if (bf.getSteps() != null) {
                    bf.getSteps()
                        .forEach(
                            step -> {
                              KnowledgeWorkflowStep workflowStep =
                                  KnowledgeWorkflowStep.builder()
                                      .sequenceNumber(step.getSequence())
                                      .stepName(step.getStepName())
                                      .actor(step.getActor())
                                      .actionText(step.getAction())
                                      .inputData(step.getInput())
                                      .outputData(step.getOutput())
                                      .nextStep(step.getNextStep())
                                      .build();
                              steps.add(workflowStep);
                            });
                  }

                  KnowledgeWorkflow workflow =
                      KnowledgeWorkflow.builder()
                          .artifactId(artifactId)
                          .projectId(source.projectId())
                          .domainId(domainId)
                          .workflowName(bf.getFlowName())
                          .triggerText(bf.getTrigger())
                          .outcomeText(bf.getOutcome())
                          .owner(bf.getOwner())
                          .confidence(bf.getConfidence())
                          .steps(steps)
                          .build();

                  workflows.add(workflow);
                });
      }
    }

    return workflows;
  }

  private void saveWorkflows(List<KnowledgeWorkflow> workflows) {
    for (KnowledgeWorkflow workflow : workflows) {
      try {
        workflow.setEmbedding(embeddingService.embedWorkflow(workflow));
        UUID workflowId = storageService.saveKnowledgeWorkflow(workflow);

        // Save workflow steps
        if (workflow.getSteps() != null) {
          for (KnowledgeWorkflowStep step : workflow.getSteps()) {
            step.setWorkflowId(workflowId);
            step.setEmbedding(embeddingService.embedWorkflowStep(step));
            storageService.saveKnowledgeWorkflowStep(step);
          }
        }
      } catch (Exception e) {
        log.error("Failed to save workflow: {}", workflow.getWorkflowName(), e);
      }
    }
    log.info("Saved {} workflows", workflows.size());
  }

  private List<KnowledgeResource> extractResources(
      String artifactId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunkKnowledge,
      Map<String, UUID> componentNameToId) {

    List<KnowledgeResource> resources = new ArrayList<>();

    for (KnowledgeExtractionResult chunkResult : chunkKnowledge) {
      if (chunkResult.getDeploymentResources() != null) {
        chunkResult
            .getDeploymentResources()
            .forEach(
                dr -> {
                  KnowledgeResource resource =
                      KnowledgeResource.builder()
                          .artifactId(artifactId)
                          .projectId(source.projectId())
                          .resourceName(dr.getResourceName())
                          .resourceType(dr.getResourceType())
                          .provider(dr.getProvider())
                          .hostingModel(dr.getHostingModel())
                          .environment(dr.getEnvironment())
                          .region(dr.getRegion())
                          .criticality(dr.getCriticality())
                          .lifecycle(dr.getLifecycle())
                          .confidence(dr.getConfidence())
                          .build();

                  // Convert configs to map
                  if (dr.getConfigs() != null) {
                    Map<String, Object> configMap = new HashMap<>();
                    dr.getConfigs()
                        .forEach(
                            config -> {
                              configMap.put(config.getKey(), config.getValue());
                            });
                    resource.setConfigs(configMap);
                  }

                  resources.add(resource);
                });
      }
    }

    return resources;
  }

  private void saveResources(List<KnowledgeResource> resources) {
    for (KnowledgeResource resource : resources) {
      try {
        resource.setEmbedding(embeddingService.embedResource(resource));
        storageService.saveKnowledgeResource(resource);
      } catch (Exception e) {
        log.error("Failed to save resource: {}", resource.getResourceName(), e);
      }
    }
    log.info("Saved {} resources", resources.size());
  }

  private List<KnowledgeRelationship> extractRelationships(
      String artifactId,
      SourceDocumentMetadata source,
      List<KnowledgeExtractionResult> chunkKnowledge) {

    List<KnowledgeRelationship> relationships = new ArrayList<>();

    for (KnowledgeExtractionResult chunkResult : chunkKnowledge) {
      if (chunkResult.getRelationships() != null) {
        relationships.addAll(chunkResult.getRelationships());
      }
    }

    return relationships;
  }

  private void saveRelationships(
      String artifactId, SourceDocumentMetadata source, List<KnowledgeRelationship> relationships) {
    for (KnowledgeRelationship relationship : relationships) {
      try {
        storageService.saveKnowledgeRelationship(artifactId, source.projectId(), relationship);
      } catch (Exception e) {
        log.error(
            "Failed to save relationship: {} -> {}",
            relationship.getSourceName(),
            relationship.getTargetName(),
            e);
      }
    }
    log.info("Saved {} relationships", relationships.size());
  }
}
