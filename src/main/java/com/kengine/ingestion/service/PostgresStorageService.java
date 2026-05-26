package com.kengine.ingestion.service;

import com.kengine.ingestion.dto.*;
import com.kengine.ingestion.entity.*;
import com.kengine.ingestion.helper.EmbeddingUtils;
import com.kengine.ingestion.repository.*;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresStorageService {

  private final SubjectRepository subjectRepository;
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
  private final EntityManager entityManager;

  public ClassificationResult findExistingClassification(SourceDocumentMetadata source) {
    // Check if document already exists - we don't skip, we'll update it
    return null;
  }

  /**
   * Deletes existing document within the same transaction as the caller. Use this when you want
   * deletion to be part of a larger transaction.
   */
  private void deleteExistingDocumentIfPresent(SourceDocumentMetadata source) {
    artifactRepository
        .findBySubjectIdAndSourceBucketAndSourceObjectAndContentHash(
            source.subjectId(), source.bucketName(), source.objectName(), source.contentHash())
        .ifPresent(
            artifact -> {
              UUID artifactId = artifact.getArtifactId();

              log.info(
                  "Deleting existing document with artifactId: {} before re-ingestion", artifactId);

              // Delete semantic chunks
              semanticChunkRepository.deleteByArtifactId(artifactId);

              // Delete ingestion documents
              ingestionDocumentRepository.deleteByArtifactId(artifactId);

              // Delete knowledge graph entities that have direct artifact_id foreign key
              documentKnowledgeRepository.deleteByArtifactId(artifactId);
              knowledgeComponentRepository.deleteByArtifactId(artifactId);
              knowledgeAPIRepository.deleteByArtifactId(artifactId);
              knowledgeBusinessRuleRepository.deleteByArtifactId(artifactId);
              knowledgeWorkflowRepository.deleteByArtifactId(artifactId);
              knowledgeDataModelRepository.deleteByArtifactId(artifactId);
              knowledgeIntegrationRepository.deleteByArtifactId(artifactId);
              knowledgeResourceRepository.deleteByArtifactId(artifactId);

              // Note: Child entities (workflow_steps, data_fields, relationships) should be
              // deleted by database CASCADE constraints on their parent foreign keys

              // Delete artifact
              artifactRepository.delete(artifact);

              // Flush and clear to ensure deletes are executed before inserting new records
              entityManager.flush();
              entityManager.clear();

              log.info("Deleted existing document and knowledge for artifact: {}", artifactId);
            });
  }

  /**
   * Deletes existing document in a separate transaction (REQUIRES_NEW). Use this when you need
   * deletion to be independent of the caller's transaction.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteExistingDocument(SourceDocumentMetadata source) {
    deleteExistingDocumentIfPresent(source);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UUID saveDocument(SourceDocumentMetadata source, List<SemanticChunk> chunks) {
    // Check if artifact already exists - if so, update it instead of creating new one
    Optional<ArtifactEntity> existingArtifactOpt =
        artifactRepository.findBySubjectIdAndSourceBucketAndSourceObjectAndContentHash(
            source.subjectId(), source.bucketName(), source.objectName(), source.contentHash());

    UUID artifactId;
    if (existingArtifactOpt.isPresent()) {
      // Artifact exists - update it by deleting old data and inserting fresh data
      artifactId = existingArtifactOpt.get().getArtifactId();
      log.info(
          "Artifact already exists for gs://{}/{}. Updating with fresh data. Artifact ID: {}",
          source.bucketName(),
          source.objectName(),
          artifactId);

      // Delete old chunks and documents (knowledge entities will be handled separately)
      semanticChunkRepository.deleteByArtifactId(artifactId);
      ingestionDocumentRepository.deleteByArtifactId(artifactId);

      // Delete old knowledge entities
      documentKnowledgeRepository.deleteByArtifactId(artifactId);
      knowledgeComponentRepository.deleteByArtifactId(artifactId);
      knowledgeAPIRepository.deleteByArtifactId(artifactId);
      knowledgeBusinessRuleRepository.deleteByArtifactId(artifactId);
      knowledgeWorkflowRepository.deleteByArtifactId(artifactId);
      knowledgeDataModelRepository.deleteByArtifactId(artifactId);
      knowledgeIntegrationRepository.deleteByArtifactId(artifactId);
      knowledgeResourceRepository.deleteByArtifactId(artifactId);

      // Update artifact metadata (entity is still managed)
      ArtifactEntity artifact = existingArtifactOpt.get();
      artifact.setUpdatedAt(java.time.OffsetDateTime.now());

      // Flush to persist the artifact update and deletes, then clear the persistence context
      // to avoid entity detachment issues when inserting new records
      entityManager.flush();
      entityManager.clear();

    } else {
      // Artifact doesn't exist - create new one
      artifactId = saveArtifact(source);
      log.info(
          "Creating new artifact for gs://{}/{}. Artifact ID: {}",
          source.bucketName(),
          source.objectName(),
          artifactId);
    }

    // Save fresh document and chunks
    UUID documentId = saveIngestionDocument(source, artifactId, chunks.size());

    for (int i = 0; i < chunks.size(); i++) {
      SemanticChunk chunk = chunks.get(i);
      saveSemanticChunk(source, chunk, artifactId, documentId, i, chunks.size());
    }

    log.info(
        "Saved document with {} chunks: gs://{}/{}",
        chunks.size(),
        source.bucketName(),
        source.objectName());

    return artifactId;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UUID findExistingArtifactInNewTransaction(SourceDocumentMetadata source) {
    return artifactRepository
        .findBySubjectIdAndSourceBucketAndSourceObjectAndContentHash(
            source.subjectId(), source.bucketName(), source.objectName(), source.contentHash())
        .map(ArtifactEntity::getArtifactId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Failed to save document and couldn't find existing artifact"));
  }

  /**
   * Cleans up duplicate artifacts for a subject (multiple copies of the same file). Keeps the most
   * recent artifact for each unique combination of source_bucket + source_object + content_hash.
   *
   * @param subjectId the subject ID to clean up
   * @return number of duplicate artifacts deleted
   */
  @Transactional
  public int cleanupDuplicateArtifacts(UUID subjectId) {
    log.info("Cleaning up duplicate artifacts for subject: {}", subjectId);

    List<ArtifactEntity> artifacts = artifactRepository.findBySubjectId(subjectId);

    if (artifacts.isEmpty()) {
      log.info("No artifacts found for subject: {}", subjectId);
      return 0;
    }

    // Group by source_bucket + source_object + content_hash to find true duplicates
    java.util.Map<String, java.util.List<ArtifactEntity>> grouped =
        artifacts.stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    a ->
                        a.getSourceBucket()
                            + "::"
                            + a.getSourceObject()
                            + "::"
                            + a.getContentHash()));

    int deletedCount = 0;

    // Delete duplicate copies (keep most recent)
    for (java.util.Map.Entry<String, java.util.List<ArtifactEntity>> entry : grouped.entrySet()) {
      java.util.List<ArtifactEntity> dupes = entry.getValue();

      if (dupes.size() > 1) {
        // Sort by creation date descending, keep the most recent one
        dupes.sort(java.util.Comparator.comparing(ArtifactEntity::getCreatedAt).reversed());

        // Delete all but the first one (most recent)
        for (int i = 1; i < dupes.size(); i++) {
          ArtifactEntity toDelete = dupes.get(i);
          UUID artifactId = toDelete.getArtifactId();

          log.info(
              "Deleting duplicate artifact: {} for file: {} (keeping newer version)",
              artifactId,
              toDelete.getSourceObject());

          // Delete associated entities
          semanticChunkRepository.deleteByArtifactId(artifactId);
          ingestionDocumentRepository.deleteByArtifactId(artifactId);
          documentKnowledgeRepository.deleteByArtifactId(artifactId);
          knowledgeComponentRepository.deleteByArtifactId(artifactId);
          knowledgeAPIRepository.deleteByArtifactId(artifactId);
          knowledgeBusinessRuleRepository.deleteByArtifactId(artifactId);
          knowledgeWorkflowRepository.deleteByArtifactId(artifactId);
          knowledgeDataModelRepository.deleteByArtifactId(artifactId);
          knowledgeIntegrationRepository.deleteByArtifactId(artifactId);
          knowledgeResourceRepository.deleteByArtifactId(artifactId);

          // Delete artifact
          artifactRepository.delete(toDelete);

          deletedCount++;

          // Flush periodically
          if (deletedCount % 10 == 0) {
            entityManager.flush();
            entityManager.clear();
          }
        }
      }
    }

    // Final flush
    if (deletedCount > 0) {
      entityManager.flush();
      entityManager.clear();
    }

    log.info(
        "Cleanup completed. Deleted {} duplicate artifacts for subject: {}",
        deletedCount,
        subjectId);

    return deletedCount;
  }

  private UUID saveArtifact(SourceDocumentMetadata source) {
    return artifactRepository
        .findBySubjectIdAndSourceBucketAndSourceObjectAndContentHash(
            source.subjectId(), source.bucketName(), source.objectName(), source.contentHash())
        .map(ArtifactEntity::getArtifactId)
        .orElseGet(
            () -> {
              ArtifactEntity artifact =
                  ArtifactEntity.builder()
                      .artifactId(UUID.randomUUID())
                      .subjectId(source.subjectId())
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

  private UUID saveIngestionDocument(
      SourceDocumentMetadata source, UUID artifactId, int chunkCount) {
    IngestionDocumentEntity document =
        IngestionDocumentEntity.builder()
            // Don't set documentId - let JPA generate it with @GeneratedValue
            .artifactId(artifactId)
            .subjectId(source.subjectId())
            .sourceBucket(source.bucketName())
            .sourceObject(source.objectName())
            .sourceGeneration(source.generation())
            .sourceChecksum(source.checksum())
            .contentHash(source.contentHash())
            .chunkCount(chunkCount)
            .build();

    return ingestionDocumentRepository.save(document).getDocumentId();
  }

  private void saveSemanticChunk(
      SourceDocumentMetadata source,
      SemanticChunk chunk,
      UUID artifactId,
      UUID documentId,
      int index,
      int total) {
    SemanticChunkEntity chunkEntity =
        SemanticChunkEntity.builder()
            .chunkId(UUID.randomUUID())
            .documentId(documentId)
            .artifactId(artifactId)
            .subjectId(source.subjectId())
            .sourceBucket(source.bucketName())
            .sourceObject(source.objectName())
            .sourceGeneration(source.generation())
            .sourceChecksum(source.checksum())
            .documentContentHash(source.contentHash())
            .chunkIndex(index)
            .totalChunks((long) total)
            .charStart((long) chunk.getCharStart())
            .charEnd((long) chunk.getCharEnd())
            .chunkContentHash(chunk.getContentHash())
            .domain(
                chunk.getClassification() != null ? chunk.getClassification().getDomain() : null)
            .subdomain(
                chunk.getClassification() != null ? chunk.getClassification().getSubdomain() : null)
            .content(chunk.getContent())
            .embedding(EmbeddingUtils.embeddingToString(chunk.getEmbedding()))
            .build();

    semanticChunkRepository.save(chunkEntity); // Custom VectorType handles the conversion
  }

  // ============================================================================
  // Knowledge Graph Persistence Methods (Phase 1 Redesign)
  // ============================================================================

  /**
   * Ensures a domain exists and returns its UUID. Creates if doesn't exist, returns existing UUID
   * if it does.
   */
  public UUID ensureDomain(UUID subjectId, String domainName) {
    return domainRepository
        .findBySubjectIdAndDomain(subjectId, domainName)
        .map(DomainEntity::getDomainId)
        .orElseGet(
            () -> {
              DomainEntity domain =
                  DomainEntity.builder().subjectId(subjectId).domain(domainName).build();
              return domainRepository.save(domain).getDomainId();
            });
  }

  /** Ensures a subdomain exists under a domain and returns its UUID. */
  public UUID ensureSubdomain(
      UUID subjectId, String domainName, UUID domainId, String subdomainName) {
    return subdomainRepository
        .findByDomainIdAndSubdomain(domainId, subdomainName)
        .map(SubdomainEntity::getSubdomainId)
        .orElseGet(
            () -> {
              SubdomainEntity subdomain =
                  SubdomainEntity.builder()
                      .subjectId(subjectId)
                      .domainId(domainId)
                      .subdomain(subdomainName)
                      .build();
              return subdomainRepository.save(subdomain).getSubdomainId();
            });
  }

  /** Saves document-level knowledge analysis results. */
  public void saveDocumentKnowledge(
      UUID artifactId, UUID subjectId, DocumentKnowledge docKnowledge) {
    // Convert List<String> to String[] for PostgreSQL array storage
    String[] keyConceptsArray =
        docKnowledge.getKeyPatterns() != null
            ? docKnowledge.getKeyPatterns().toArray(new String[0])
            : null;
    String[] technologiesArray =
        docKnowledge.getTechnologies() != null
            ? docKnowledge.getTechnologies().toArray(new String[0])
            : null;
    String[] identifiedComponentsArray =
        docKnowledge.getIdentifiedComponents() != null
            ? docKnowledge.getIdentifiedComponents().toArray(new String[0])
            : null;
    String[] identifiedApisArray =
        docKnowledge.getIdentifiedAPIs() != null
            ? docKnowledge.getIdentifiedAPIs().toArray(new String[0])
            : null;
    String[] identifiedWorkflowsArray =
        docKnowledge.getIdentifiedWorkflows() != null
            ? docKnowledge.getIdentifiedWorkflows().toArray(new String[0])
            : null;
    String[] identifiedCapabilitiesArray =
        docKnowledge.getIdentifiedCapabilities() != null
            ? docKnowledge.getIdentifiedCapabilities().toArray(new String[0])
            : null;
    String[] identifiedRolesArray =
        docKnowledge.getIdentifiedRoles() != null
            ? docKnowledge.getIdentifiedRoles().toArray(new String[0])
            : null;
    String[] identifiedTermsArray =
        docKnowledge.getIdentifiedTerms() != null
            ? docKnowledge.getIdentifiedTerms().toArray(new String[0])
            : null;
    String[] identifiedPoliciesArray =
        docKnowledge.getIdentifiedPolicies() != null
            ? docKnowledge.getIdentifiedPolicies().toArray(new String[0])
            : null;
    String[] identifiedDecisionsArray =
        docKnowledge.getIdentifiedDecisions() != null
            ? docKnowledge.getIdentifiedDecisions().toArray(new String[0])
            : null;

    DocumentKnowledgeEntity entity =
        DocumentKnowledgeEntity.builder()
            .artifactId(artifactId)
            .subjectId(subjectId)
            .title(docKnowledge.getSystemSummary())
            .summary(docKnowledge.getSystemSummary())
            .overallArchitecture(docKnowledge.getOverallArchitecture())
            .domain(docKnowledge.getDomain())
            .subdomain(docKnowledge.getSubdomain())
            .documentType(docKnowledge.getDetectedPlatform())
            .keyConcepts(keyConceptsArray)
            .technologies(technologiesArray)
            .identifiedComponents(identifiedComponentsArray)
            .identifiedApis(identifiedApisArray)
            .identifiedWorkflows(identifiedWorkflowsArray)
            .identifiedCapabilities(identifiedCapabilitiesArray)
            .identifiedRoles(identifiedRolesArray)
            .identifiedTerms(identifiedTermsArray)
            .identifiedPolicies(identifiedPoliciesArray)
            .identifiedDecisions(identifiedDecisionsArray)
            .extractedAt(java.time.OffsetDateTime.now())
            .build();

    documentKnowledgeRepository.save(entity);
  }

  /** Saves a knowledge component and returns its UUID. */
  public UUID saveKnowledgeComponent(KnowledgeComponent component) {
    KnowledgeComponentEntity entity =
        KnowledgeComponentEntity.builder()
            .artifactId(component.getArtifactId())
            .subjectId(component.getSubjectId())
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
            .embedding(EmbeddingUtils.embeddingToString(component.getEmbedding()))
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
            .subjectId(api.getSubjectId())
            .apiName(api.getApiName())
            .apiType(api.getApiType())
            .httpMethod(api.getHttpMethod())
            .endpointPath(api.getEndpointPath())
            .description(api.getDescription())
            .requestSchema(api.getRequestSchema())
            .responseSchema(api.getResponseSchema())
            .authentication(api.getAuthentication())
            .embedding(EmbeddingUtils.embeddingToString(api.getEmbedding()))
            .build();

    return knowledgeAPIRepository.save(entity).getApiId();
  }

  /** Saves a knowledge business rule and returns its UUID. */
  public UUID saveKnowledgeBusinessRule(KnowledgeBusinessRule rule) {
    KnowledgeBusinessRuleEntity entity =
        KnowledgeBusinessRuleEntity.builder()
            .artifactId(rule.getArtifactId())
            .componentId(rule.getComponentId())
            .subjectId(rule.getSubjectId())
            .domainId(rule.getDomainId())
            .ruleName(rule.getRuleName())
            .ruleType(rule.getRuleType())
            .conditionText(rule.getConditionText())
            .outcomeText(rule.getOutcomeText())
            .priority(rule.getPriority())
            .confidence(rule.getConfidence())
            .embedding(EmbeddingUtils.embeddingToString(rule.getEmbedding()))
            .technicalImplementation(rule.getTechnicalImplementation())
            .validationCriteria(rule.getValidationCriteria())
            .build();

    return knowledgeBusinessRuleRepository.save(entity).getRuleId();
  }

  /** Saves a knowledge workflow and returns its UUID. */
  public UUID saveKnowledgeWorkflow(KnowledgeWorkflow workflow) {
    KnowledgeWorkflowEntity entity =
        KnowledgeWorkflowEntity.builder()
            .artifactId(workflow.getArtifactId())
            .subjectId(workflow.getSubjectId())
            .domainId(workflow.getDomainId())
            .workflowName(workflow.getWorkflowName())
            .triggerText(workflow.getTriggerText())
            .outcomeText(workflow.getOutcomeText())
            .owner(workflow.getOwner())
            .confidence(workflow.getConfidence())
            .embedding(EmbeddingUtils.embeddingToString(workflow.getEmbedding()))
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
            .embedding(EmbeddingUtils.embeddingToString(step.getEmbedding()))
            .technicalDetails(step.getTechnicalDetails())
            .inputParameters(step.getInputParameters())
            .outputParameters(step.getOutputParameters())
            .build();

    return knowledgeWorkflowStepRepository.save(entity).getStepId();
  }

  /** Saves a knowledge data model and returns its UUID. */
  public UUID saveKnowledgeDataModel(KnowledgeDataModel dataModel) {
    KnowledgeDataModelEntity entity =
        KnowledgeDataModelEntity.builder()
            .artifactId(dataModel.getArtifactId())
            .subjectId(dataModel.getSubjectId())
            .domainId(dataModel.getDomainId())
            .modelName(dataModel.getModelName())
            .modelType(dataModel.getModelType())
            .description(dataModel.getDescription())
            .schemaDefinition(dataModel.getSchemaDefinition())
            .embedding(EmbeddingUtils.embeddingToString(dataModel.getEmbedding()))
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
            .subjectId(integration.getSubjectId())
            .integrationName(integration.getIntegrationName())
            .integrationType(integration.getIntegrationType())
            .sourceSystem(integration.getSourceSystem())
            .targetSystem(integration.getTargetSystem())
            .protocol(integration.getProtocol())
            .description(integration.getDescription())
            .embedding(EmbeddingUtils.embeddingToString(integration.getEmbedding()))
            .build();

    return knowledgeIntegrationRepository.save(entity).getIntegrationId();
  }

  /** Saves a knowledge resource and returns its UUID. */
  public UUID saveKnowledgeResource(KnowledgeResource resource) {
    KnowledgeResourceEntity entity =
        KnowledgeResourceEntity.builder()
            .artifactId(resource.getArtifactId())
            .componentId(resource.getComponentId())
            .subjectId(resource.getSubjectId())
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
            .embedding(EmbeddingUtils.embeddingToString(resource.getEmbedding()))
            .build();

    return knowledgeResourceRepository.save(entity).getResourceId();
  }

  /** Saves a knowledge relationship. */
  public void saveKnowledgeRelationship(
      UUID artifactId, UUID subjectId, KnowledgeRelationship relationship) {
    KnowledgeRelationshipEntity entity =
        KnowledgeRelationshipEntity.builder()
            .relationshipId(UUID.randomUUID().toString())
            .subjectId(subjectId)
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
