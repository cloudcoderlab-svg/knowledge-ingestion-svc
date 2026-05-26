package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
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
      UUID artifactId,
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
        domainId = ensureDomain(source.subjectId(), docKnowledge.getDomain());

        if (docKnowledge.getSubdomain() != null) {
          subdomainId =
              ensureSubdomain(
                  source.subjectId(),
                  docKnowledge.getDomain(),
                  domainId,
                  docKnowledge.getSubdomain());
        }
      }

      // 2. Save document-level knowledge
      if (docKnowledge != null) {
        saveDocumentKnowledge(artifactId, source.subjectId(), docKnowledge);
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
  public UUID ensureDomain(UUID subjectId, String domainName) {
    try {
      return storageService.ensureDomain(subjectId, domainName);
    } catch (Exception e) {
      log.error("Failed to ensure domain: {}", domainName, e);
      throw new RuntimeException("Failed to ensure domain", e);
    }
  }

  /** Ensures a subdomain exists under a domain and returns its UUID. */
  public UUID ensureSubdomain(
      UUID subjectId, String domainName, UUID domainId, String subdomainName) {
    try {
      return storageService.ensureSubdomain(subjectId, domainName, domainId, subdomainName);
    } catch (Exception e) {
      log.error("Failed to ensure subdomain: {}", subdomainName, e);
      throw new RuntimeException("Failed to ensure subdomain", e);
    }
  }

  private void saveDocumentKnowledge(
      UUID artifactId, UUID subjectId, DocumentKnowledge docKnowledge) {
    try {
      storageService.saveDocumentKnowledge(artifactId, subjectId, docKnowledge);
      log.debug("Saved document knowledge for artifact: {}", artifactId);
    } catch (Exception e) {
      log.error("Failed to save document knowledge", e);
    }
  }

  private List<KnowledgeComponent> extractComponents(
      UUID artifactId,
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
                          .subjectId(source.subjectId())
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
                          .subjectId(source.subjectId())
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

  /**
   * Generic template method for saving knowledge entities.
   *
   * <p>Eliminates code duplication across save methods by providing a reusable pattern for:
   *
   * <ul>
   *   <li>Iterating over entities
   *   <li>Optional embedding generation
   *   <li>Saving to storage
   *   <li>Error handling and logging
   * </ul>
   *
   * @param <T> Type of entity being saved
   * @param entities List of entities to save
   * @param entityName Human-readable name for logging (e.g., "component", "business rule")
   * @param embeddingGenerator Optional function to generate embedding (null if not needed)
   * @param entitySaver Function to save entity and return its ID
   * @param entityNameExtractor Function to extract entity name for error logging
   */
  private <T> void saveEntities(
      List<T> entities,
      String entityName,
      Consumer<T> embeddingGenerator,
      Function<T, UUID> entitySaver,
      Function<T, String> entityNameExtractor) {

    for (T entity : entities) {
      try {
        // Generate embedding if generator provided
        if (embeddingGenerator != null) {
          embeddingGenerator.accept(entity);
        }

        // Save entity
        entitySaver.apply(entity);

      } catch (Exception e) {
        String name = entityNameExtractor != null ? entityNameExtractor.apply(entity) : "unknown";
        log.error("Failed to save {}: {}", entityName, name, e);
      }
    }

    log.info("Saved {} {}{}", entities.size(), entityName, entities.size() != 1 ? "s" : "");
  }

  private Map<String, UUID> saveComponents(List<KnowledgeComponent> components) {
    Map<String, UUID> nameToId = new HashMap<>();

    for (KnowledgeComponent component : components) {
      try {
        // Generate embedding
        component.setEmbedding(embeddingService.embedComponent(component));

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
      UUID artifactId,
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
                          .subjectId(source.subjectId())
                          .domainId(domainId)
                          .ruleName(br.getRuleName())
                          .ruleType(br.getRuleType())
                          .conditionText(br.getCondition())
                          .outcomeText(br.getOutcome())
                          .priority(br.getPriority())
                          .confidence(br.getConfidence())
                          .technicalImplementation(br.getTechnicalImplementation())
                          .validationCriteria(br.getValidationCriteria())
                          .build();

                  rules.add(rule);
                });
      }
    }

    return rules;
  }

  private void saveBusinessRules(List<KnowledgeBusinessRule> rules) {
    saveEntities(
        rules,
        "business rule",
        rule -> rule.setEmbedding(embeddingService.embedBusinessRule(rule)),
        rule -> {
          storageService.saveKnowledgeBusinessRule(rule);
          return null; // Business rules don't return ID in this context
        },
        KnowledgeBusinessRule::getRuleName);
  }

  private List<KnowledgeWorkflow> extractWorkflows(
      UUID artifactId,
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
                                      .technicalDetails(step.getTechnicalDetails())
                                      .inputParameters(step.getInputParameters())
                                      .outputParameters(step.getOutputParameters())
                                      .build();
                              steps.add(workflowStep);
                            });
                  }

                  KnowledgeWorkflow workflow =
                      KnowledgeWorkflow.builder()
                          .artifactId(artifactId)
                          .subjectId(source.subjectId())
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
        // Generate workflow embedding
        workflow.setEmbedding(embeddingService.embedWorkflow(workflow));

        // Save workflow and get ID
        UUID workflowId = storageService.saveKnowledgeWorkflow(workflow);

        // Save workflow steps if present
        if (workflow.getSteps() != null) {
          saveWorkflowSteps(workflowId, workflow.getSteps());
        }
      } catch (Exception e) {
        log.error("Failed to save workflow: {}", workflow.getWorkflowName(), e);
      }
    }
    log.info("Saved {} workflows", workflows.size());
  }

  /**
   * Saves workflow steps for a workflow.
   *
   * @param workflowId UUID of the parent workflow
   * @param steps List of workflow steps to save
   */
  private void saveWorkflowSteps(UUID workflowId, List<KnowledgeWorkflowStep> steps) {
    saveEntities(
        steps,
        "workflow step",
        step -> {
          step.setWorkflowId(workflowId);
          step.setEmbedding(embeddingService.embedWorkflowStep(step));
        },
        step -> {
          storageService.saveKnowledgeWorkflowStep(step);
          return null;
        },
        KnowledgeWorkflowStep::getStepName);
  }

  private List<KnowledgeResource> extractResources(
      UUID artifactId,
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
                          .subjectId(source.subjectId())
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
    saveEntities(
        resources,
        "resource",
        resource -> resource.setEmbedding(embeddingService.embedResource(resource)),
        resource -> {
          storageService.saveKnowledgeResource(resource);
          return null;
        },
        KnowledgeResource::getResourceName);
  }

  private List<KnowledgeRelationship> extractRelationships(
      UUID artifactId,
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
      UUID artifactId, SourceDocumentMetadata source, List<KnowledgeRelationship> relationships) {
    saveEntities(
        relationships,
        "relationship",
        null, // No embedding generation for relationships
        relationship -> {
          storageService.saveKnowledgeRelationship(artifactId, source.subjectId(), relationship);
          return null;
        },
        rel -> rel.getSourceName() + " -> " + rel.getTargetName());
  }
}
