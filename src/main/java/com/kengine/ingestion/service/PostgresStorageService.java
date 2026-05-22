package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.*;
import com.kengine.ingestion.entity.*;
import com.kengine.ingestion.repository.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresStorageService {

  private final ProjectRepository projectRepository;
  private final ArtifactRepository artifactRepository;
  private final IngestionDocumentRepository ingestionDocumentRepository;
  private final SemanticChunkRepository semanticChunkRepository;
  private final DomainRepository domainRepository;
  private final SubdomainRepository subdomainRepository;
  private final DocumentKnowledgeRepository documentKnowledgeRepository;
  private final KnowledgeComponentRepository knowledgeComponentRepository;
  private final KnowledgeAPIRepository knowledgeAPIRepository;
  private final KnowledgeBusinessRuleRepository knowledgeBusinessRuleRepository;
  private final KnowledgeWorkflowRepository knowledgeWorkflowRepository;
  private final KnowledgeWorkflowStepRepository knowledgeWorkflowStepRepository;
  private final KnowledgeDataModelRepository knowledgeDataModelRepository;
  private final KnowledgeDataFieldRepository knowledgeDataFieldRepository;
  private final KnowledgeIntegrationRepository knowledgeIntegrationRepository;
  private final KnowledgeResourceRepository knowledgeResourceRepository;
  private final KnowledgeRelationshipRepository knowledgeRelationshipRepository;

  public ClassificationResult findExistingClassification(SourceDocumentMetadata source) {
    boolean exists =
        ingestionDocumentRepository.existsByProjectIdAndSourceBucketAndSourceObjectAndContentHash(
            source.projectId(), source.bucketName(), source.objectName(), source.contentHash());

    if (exists) {
      log.info("Found existing document for hash: {}", source.contentHash());
      return new ClassificationResult();
    }
    return null;
  }

  @Transactional
  public void saveDocument(SourceDocumentMetadata source, List<SemanticChunk> chunks) {
    ensureProjectExists(source.projectId());

    String artifactId = saveArtifact(source);
    String documentId = saveIngestionDocument(source, artifactId, chunks.size());

    for (int i = 0; i < chunks.size(); i++) {
      SemanticChunk chunk = chunks.get(i);
      saveSemanticChunk(source, chunk, artifactId, documentId, i, chunks.size());
    }

    log.info(
        "Saved document with {} chunks: gs://{}/{}",
        chunks.size(),
        source.bucketName(),
        source.objectName());
  }

  private void ensureProjectExists(String projectId) {
    projectRepository
        .findByProjectId(projectId)
        .orElseGet(
            () -> {
              ProjectEntity project =
                  ProjectEntity.builder()
                      .projectId(projectId)
                      .projectName(projectId)
                      .sourceBucket("default-bucket")
                      .gcsPrefix(projectId + "/")
                      .build();
              return projectRepository.save(project);
            });
  }

  private String saveArtifact(SourceDocumentMetadata source) {
    return artifactRepository
        .findByProjectIdAndSourceBucketAndSourceObjectAndContentHash(
            source.projectId(), source.bucketName(), source.objectName(), source.contentHash())
        .map(ArtifactEntity::getArtifactId)
        .orElseGet(
            () -> {
              ArtifactEntity artifact =
                  ArtifactEntity.builder()
                      .artifactId(UUID.randomUUID().toString())
                      .projectId(source.projectId())
                      .sourceBucket(source.bucketName())
                      .sourceObject(source.objectName())
                      .sourceGeneration(source.generation())
                      .sourceChecksum(source.checksum())
                      .contentHash(source.contentHash())
                      .artifactType(source.artifactType())
                      .fileType(source.fileType())
                      .title(source.title())
                      .isCurrent(true)
                      .build();
              return artifactRepository.save(artifact).getArtifactId();
            });
  }

  private String saveIngestionDocument(
      SourceDocumentMetadata source, String artifactId, int chunkCount) {
    IngestionDocumentEntity document =
        IngestionDocumentEntity.builder()
            .documentId(UUID.randomUUID().toString())
            .artifactId(artifactId)
            .projectId(source.projectId())
            .sourceBucket(source.bucketName())
            .sourceObject(source.objectName())
            .sourceGeneration(source.generation())
            .sourceChecksum(source.checksum())
            .contentHash(source.contentHash())
            .chunkCount((long) chunkCount)
            .build();

    return ingestionDocumentRepository.save(document).getDocumentId();
  }

  private void saveSemanticChunk(
      SourceDocumentMetadata source,
      SemanticChunk chunk,
      String artifactId,
      String documentId,
      int index,
      int total) {
    SemanticChunkEntity chunkEntity =
        SemanticChunkEntity.builder()
            .chunkId(UUID.randomUUID().toString())
            .documentId(documentId)
            .artifactId(artifactId)
            .projectId(source.projectId())
            .sourceBucket(source.bucketName())
            .sourceObject(source.objectName())
            .sourceGeneration(source.generation())
            .sourceChecksum(source.checksum())
            .documentContentHash(source.contentHash())
            .chunkIndex((long) index)
            .totalChunks((long) total)
            .charStart((long) chunk.getCharStart())
            .charEnd((long) chunk.getCharEnd())
            .chunkContentHash(chunk.getContentHash())
            .content(chunk.getContent())
            .embedding(chunk.getEmbedding())
            .build();

    semanticChunkRepository.save(chunkEntity);
  }

  // ============================================================================
  // Knowledge Graph Persistence Methods (Phase 1 Redesign)
  // ============================================================================

  /**
   * Ensures a domain exists and returns its UUID. Creates if doesn't exist, returns existing UUID
   * if it does.
   */
  public UUID ensureDomain(String projectId, String domainName) {
    return domainRepository
        .findByProjectIdAndDomain(projectId, domainName)
        .map(DomainEntity::getDomainId)
        .orElseGet(
            () -> {
              DomainEntity domain =
                  DomainEntity.builder().projectId(projectId).domain(domainName).build();
              return domainRepository.save(domain).getDomainId();
            });
  }

  /** Ensures a subdomain exists under a domain and returns its UUID. */
  public UUID ensureSubdomain(UUID domainId, String subdomainName) {
    return subdomainRepository
        .findByDomainIdAndSubdomain(domainId, subdomainName)
        .map(SubdomainEntity::getSubdomainId)
        .orElseGet(
            () -> {
              SubdomainEntity subdomain =
                  SubdomainEntity.builder().domainId(domainId).subdomain(subdomainName).build();
              return subdomainRepository.save(subdomain).getSubdomainId();
            });
  }

  /** Saves document-level knowledge analysis results. */
  public void saveDocumentKnowledge(String artifactId, DocumentKnowledge docKnowledge) {
    // Convert List<String> to Map<String, Object> for JSONB storage
    Map<String, Object> keyPatternsMap =
        docKnowledge.getKeyPatterns() != null
            ? Map.of("patterns", docKnowledge.getKeyPatterns())
            : null;
    Map<String, Object> technologiesMap =
        docKnowledge.getTechnologies() != null
            ? Map.of("technologies", docKnowledge.getTechnologies())
            : null;

    DocumentKnowledgeEntity entity =
        DocumentKnowledgeEntity.builder()
            .artifactId(artifactId)
            .overallArchitecture(docKnowledge.getOverallArchitecture())
            .systemSummary(docKnowledge.getSystemSummary())
            .keyPatterns(keyPatternsMap)
            .technologies(technologiesMap)
            .build();

    documentKnowledgeRepository.save(entity);
  }

  /** Saves a knowledge component and returns its UUID. */
  public UUID saveKnowledgeComponent(KnowledgeComponent component) {
    KnowledgeComponentEntity entity =
        KnowledgeComponentEntity.builder()
            .artifactId(component.getArtifactId())
            .projectId(component.getProjectId())
            .domainId(component.getDomainId())
            .subdomainId(component.getSubdomainId())
            .componentName(component.getComponentName())
            .componentType(component.getComponentType())
            .category(component.getCategory())
            .description(component.getDescription())
            .responsibility(component.getResponsibility())
            .technology(component.getTechnology())
            .capability(component.getCapability())
            .owner(component.getOwner())
            .lifecycle(component.getLifecycle())
            .confidence(component.getConfidence())
            .embedding(component.getEmbedding())
            .metadata(component.getMetadata())
            .build();

    return knowledgeComponentRepository.save(entity).getComponentId();
  }

  /** Saves a knowledge API and returns its UUID. */
  public UUID saveKnowledgeAPI(KnowledgeAPI api) {
    KnowledgeAPIEntity entity =
        KnowledgeAPIEntity.builder()
            .componentId(api.getComponentId())
            .artifactId(api.getArtifactId())
            .projectId(api.getProjectId())
            .apiName(api.getApiName())
            .apiType(api.getApiType())
            .httpMethod(api.getHttpMethod())
            .endpointPath(api.getEndpointPath())
            .description(api.getDescription())
            .requestSchema(api.getRequestSchema())
            .responseSchema(api.getResponseSchema())
            .authentication(api.getAuthentication())
            .embedding(api.getEmbedding())
            .build();

    return knowledgeAPIRepository.save(entity).getApiId();
  }

  /** Saves a knowledge business rule and returns its UUID. */
  public UUID saveKnowledgeBusinessRule(KnowledgeBusinessRule rule) {
    KnowledgeBusinessRuleEntity entity =
        KnowledgeBusinessRuleEntity.builder()
            .artifactId(rule.getArtifactId())
            .componentId(rule.getComponentId())
            .projectId(rule.getProjectId())
            .domainId(rule.getDomainId())
            .ruleName(rule.getRuleName())
            .ruleType(rule.getRuleType())
            .conditionText(rule.getConditionText())
            .outcomeText(rule.getOutcomeText())
            .priority(rule.getPriority())
            .confidence(rule.getConfidence())
            .embedding(rule.getEmbedding())
            .build();

    return knowledgeBusinessRuleRepository.save(entity).getRuleId();
  }

  /** Saves a knowledge workflow and returns its UUID. */
  public UUID saveKnowledgeWorkflow(KnowledgeWorkflow workflow) {
    KnowledgeWorkflowEntity entity =
        KnowledgeWorkflowEntity.builder()
            .artifactId(workflow.getArtifactId())
            .projectId(workflow.getProjectId())
            .domainId(workflow.getDomainId())
            .workflowName(workflow.getWorkflowName())
            .triggerText(workflow.getTriggerText())
            .outcomeText(workflow.getOutcomeText())
            .owner(workflow.getOwner())
            .confidence(workflow.getConfidence())
            .embedding(workflow.getEmbedding())
            .build();

    return knowledgeWorkflowRepository.save(entity).getWorkflowId();
  }

  /** Saves a knowledge workflow step and returns its UUID. */
  public UUID saveKnowledgeWorkflowStep(KnowledgeWorkflowStep step) {
    KnowledgeWorkflowStepEntity entity =
        KnowledgeWorkflowStepEntity.builder()
            .workflowId(step.getWorkflowId())
            .sequenceNumber(step.getSequenceNumber())
            .stepName(step.getStepName())
            .actor(step.getActor())
            .actionText(step.getActionText())
            .inputData(step.getInputData())
            .outputData(step.getOutputData())
            .nextStep(step.getNextStep())
            .embedding(step.getEmbedding())
            .build();

    return knowledgeWorkflowStepRepository.save(entity).getStepId();
  }

  /** Saves a knowledge data model and returns its UUID. */
  public UUID saveKnowledgeDataModel(KnowledgeDataModel dataModel) {
    KnowledgeDataModelEntity entity =
        KnowledgeDataModelEntity.builder()
            .artifactId(dataModel.getArtifactId())
            .projectId(dataModel.getProjectId())
            .domainId(dataModel.getDomainId())
            .modelName(dataModel.getModelName())
            .modelType(dataModel.getModelType())
            .description(dataModel.getDescription())
            .schemaDefinition(dataModel.getSchemaDefinition())
            .embedding(dataModel.getEmbedding())
            .build();

    return knowledgeDataModelRepository.save(entity).getDataModelId();
  }

  /** Saves a knowledge data field and returns its UUID. */
  public UUID saveKnowledgeDataField(KnowledgeDataField field) {
    KnowledgeDataFieldEntity entity =
        KnowledgeDataFieldEntity.builder()
            .dataModelId(field.getDataModelId())
            .fieldName(field.getFieldName())
            .fieldType(field.getFieldType())
            .isRequired(field.getIsRequired())
            .description(field.getDescription())
            .constraints(field.getConstraints())
            .build();

    return knowledgeDataFieldRepository.save(entity).getFieldId();
  }

  /** Saves a knowledge integration and returns its UUID. */
  public UUID saveKnowledgeIntegration(KnowledgeIntegration integration) {
    KnowledgeIntegrationEntity entity =
        KnowledgeIntegrationEntity.builder()
            .artifactId(integration.getArtifactId())
            .componentId(integration.getComponentId())
            .projectId(integration.getProjectId())
            .integrationName(integration.getIntegrationName())
            .integrationType(integration.getIntegrationType())
            .sourceSystem(integration.getSourceSystem())
            .targetSystem(integration.getTargetSystem())
            .protocol(integration.getProtocol())
            .description(integration.getDescription())
            .embedding(integration.getEmbedding())
            .build();

    return knowledgeIntegrationRepository.save(entity).getIntegrationId();
  }

  /** Saves a knowledge resource and returns its UUID. */
  public UUID saveKnowledgeResource(KnowledgeResource resource) {
    KnowledgeResourceEntity entity =
        KnowledgeResourceEntity.builder()
            .artifactId(resource.getArtifactId())
            .componentId(resource.getComponentId())
            .projectId(resource.getProjectId())
            .resourceName(resource.getResourceName())
            .resourceType(resource.getResourceType())
            .provider(resource.getProvider())
            .hostingModel(resource.getHostingModel())
            .environment(resource.getEnvironment())
            .region(resource.getRegion())
            .criticality(resource.getCriticality())
            .lifecycle(resource.getLifecycle())
            .configs(resource.getConfigs())
            .confidence(resource.getConfidence())
            .embedding(resource.getEmbedding())
            .build();

    return knowledgeResourceRepository.save(entity).getResourceId();
  }

  /** Saves a knowledge relationship. */
  public void saveKnowledgeRelationship(
      String artifactId, String projectId, KnowledgeRelationship relationship) {
    KnowledgeRelationshipEntity entity =
        KnowledgeRelationshipEntity.builder()
            .relationshipId(UUID.randomUUID().toString())
            .projectId(projectId)
            .sourceName(relationship.getSourceName())
            .sourceType(relationship.getSourceType())
            .sourceRefId(null) // Not available in DTO
            .targetName(relationship.getTargetName())
            .targetType(relationship.getTargetType())
            .targetRefId(null) // Not available in DTO
            .relationshipType(relationship.getRelationshipType())
            .context(relationship.getContext())
            .sourceArtifactId(artifactId)
            .confidence(relationship.getConfidence())
            .build();

    knowledgeRelationshipRepository.save(entity);
  }
}
