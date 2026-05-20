package com.kengine.ingestion.service;

import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.kengine.ingestion.dto.BusinessComponent;
import com.kengine.ingestion.dto.BusinessFlow;
import com.kengine.ingestion.dto.BusinessFlowStep;
import com.kengine.ingestion.dto.BusinessRule;
import com.kengine.ingestion.dto.ClassificationResult;
import com.kengine.ingestion.dto.DeploymentResource;
import com.kengine.ingestion.dto.KnowledgeExtractionResult;
import com.kengine.ingestion.dto.KnowledgeRelationship;
import com.kengine.ingestion.dto.ResourceConfig;
import com.kengine.ingestion.dto.ResourceCostEstimate;
import com.kengine.ingestion.dto.SemanticChunk;
import com.kengine.ingestion.dto.SourceDocumentMetadata;
import com.kengine.ingestion.dto.TechnicalComponent;
import com.kengine.ingestion.dto.UsageProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpannerStorageService {

  private final DatabaseClient databaseClient;

  public ClassificationResult findExistingClassification(SourceDocumentMetadata source) {
    Statement statement =
        Statement.newBuilder(
                "SELECT domain, subdomain "
                    + "FROM knowledge.semantic_chunks "
                    + "WHERE project_id = $1 "
                    + "AND source_bucket = $2 "
                    + "AND source_object = $3 "
                    + "AND document_content_hash = $4 "
                    + "ORDER BY chunk_index ASC "
                    + "LIMIT 1")
            .bind("p1")
            .to(source.projectId())
            .bind("p2")
            .to(source.bucketName())
            .bind("p3")
            .to(source.objectName())
            .bind("p4")
            .to(source.contentHash())
            .build();

    try (ResultSet rows = databaseClient.singleUse().executeQuery(statement)) {
      if (!rows.next()) {
        return null;
      }
      ClassificationResult result = new ClassificationResult();
      result.setDomain(rows.getString("domain"));
      result.setSubdomain(rows.getString("subdomain"));
      return result;
    }
  }

  public void saveDocument(SourceDocumentMetadata source, List<SemanticChunk> chunks) {
    SemanticChunk firstChunk = chunks.getFirst();
    String documentId = UUID.randomUUID().toString();
    String artifactId = UUID.randomUUID().toString();
    List<Mutation> mutations = new ArrayList<>();
    mutations.add(projectMutation(source));
    mutations.add(domainMutation(source, firstChunk));
    mutations.add(subdomainMutation(source, firstChunk));
    mutations.add(artifactMutation(artifactId, source, firstChunk));
    mutations.add(documentMutation(documentId, artifactId, source, chunks.size()));

    Map<String, String> componentIds = new HashMap<>();
    Map<String, String> resourceIds = new HashMap<>();
    for (SemanticChunk chunk : chunks) {
      String chunkId = UUID.randomUUID().toString();
      mutations.add(domainMutation(source, chunk));
      mutations.add(subdomainMutation(source, chunk));
      mutations.add(chunkMutation(chunkId, documentId, artifactId, source, chunk));
      mutations.addAll(
          knowledgeMutations(source, artifactId, chunkId, chunk, componentIds, resourceIds));
    }

    databaseClient.write(mutations);
  }

  private List<Mutation> knowledgeMutations(
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      SemanticChunk chunk,
      Map<String, String> componentIds,
      Map<String, String> resourceIds) {
    KnowledgeExtractionResult extraction = chunk.getKnowledgeExtraction();
    if (extraction == null) {
      return List.of();
    }

    List<Mutation> mutations = new ArrayList<>();
    addNote(
        mutations,
        source,
        artifactId,
        chunkId,
        "architectural_summary",
        extraction.getArchitecturalSummary());
    mutations.addAll(
        businessComponentMutations(source, artifactId, chunkId, chunk, extraction, componentIds));
    mutations.addAll(
        technicalComponentMutations(source, artifactId, chunkId, chunk, extraction, componentIds));
    mutations.addAll(businessRuleMutations(source, artifactId, chunkId, extraction));
    mutations.addAll(businessFlowMutations(source, artifactId, chunkId, extraction));
    mutations.addAll(
        deploymentResourceMutations(source, artifactId, chunkId, chunk, extraction, resourceIds));
    mutations.addAll(
        relationshipMutations(source, artifactId, chunkId, extraction, componentIds, resourceIds));
    mutations.addAll(usageProfileMutations(source, chunkId, extraction));
    mutations.addAll(costEstimateMutations(source, artifactId, chunkId, resourceIds, extraction));
    if (extraction.getMigrationNotes() != null) {
      for (String note : extraction.getMigrationNotes()) {
        addNote(mutations, source, artifactId, chunkId, "migration_note", note);
      }
    }
    return mutations;
  }

  private List<Mutation> businessComponentMutations(
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      SemanticChunk chunk,
      KnowledgeExtractionResult extraction,
      Map<String, String> componentIds) {
    if (extraction.getBusinessComponents() == null) {
      return List.of();
    }
    List<Mutation> mutations = new ArrayList<>();
    for (BusinessComponent component : extraction.getBusinessComponents()) {
      if (isBlank(component.getComponentName())) {
        continue;
      }
      String componentId =
          componentIds.computeIfAbsent(
              componentKey("business", component.getComponentName(), component.getComponentType()),
              ignored -> UUID.randomUUID().toString());
      ClassificationResult classification = chunk.getClassification();
      mutations.add(
          solutionComponentMutation(
              componentId,
              source,
              artifactId,
              chunkId,
              classification,
              "business",
              component.getComponentName(),
              component.getComponentType(),
              component.getCapability(),
              null,
              null,
              component.getOwner(),
              component.getLifecycle(),
              component.getConfidence()));
    }
    return mutations;
  }

  private List<Mutation> technicalComponentMutations(
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      SemanticChunk chunk,
      KnowledgeExtractionResult extraction,
      Map<String, String> componentIds) {
    if (extraction.getTechnicalComponents() == null) {
      return List.of();
    }
    List<Mutation> mutations = new ArrayList<>();
    for (TechnicalComponent component : extraction.getTechnicalComponents()) {
      if (isBlank(component.getComponentName())) {
        continue;
      }
      String componentId =
          componentIds.computeIfAbsent(
              componentKey("technical", component.getComponentName(), component.getComponentType()),
              ignored -> UUID.randomUUID().toString());
      ClassificationResult classification = chunk.getClassification();
      mutations.add(
          solutionComponentMutation(
              componentId,
              source,
              artifactId,
              chunkId,
              classification,
              "technical",
              component.getComponentName(),
              component.getComponentType(),
              null,
              component.getResponsibility(),
              component.getTechnology(),
              null,
              component.getLifecycle(),
              component.getConfidence()));
    }
    return mutations;
  }

  private Mutation solutionComponentMutation(
      String componentId,
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      ClassificationResult classification,
      String componentLayer,
      String componentName,
      String componentType,
      String capability,
      String responsibility,
      String technology,
      String owner,
      String lifecycle,
      Double confidence) {
    return Mutation.newInsertOrUpdateBuilder("knowledge.solution_components")
        .set("component_id")
        .to(componentId)
        .set("project_id")
        .to(source.projectId())
        .set("domain")
        .to(domain(classification))
        .set("subdomain")
        .to(subdomain(classification))
        .set("component_layer")
        .to(componentLayer)
        .set("component_name")
        .to(componentName)
        .set("component_type")
        .to(firstNonBlank(componentType, "unknown"))
        .set("capability")
        .to(capability)
        .set("responsibility")
        .to(responsibility)
        .set("technology")
        .to(technology)
        .set("owner")
        .to(owner)
        .set("lifecycle")
        .to(firstNonBlank(lifecycle, "unknown"))
        .set("source_artifact_id")
        .to(artifactId)
        .set("source_chunk_id")
        .to(chunkId)
        .set("confidence")
        .to(confidence)
        .set("created_at")
        .to(Timestamp.now())
        .set("updated_at")
        .to(Timestamp.now())
        .build();
  }

  private List<Mutation> businessRuleMutations(
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      KnowledgeExtractionResult extraction) {
    if (extraction.getBusinessRules() == null) {
      return List.of();
    }
    List<Mutation> mutations = new ArrayList<>();
    for (BusinessRule rule : extraction.getBusinessRules()) {
      if (isBlank(rule.getRuleName()) && isBlank(rule.getCondition())) {
        continue;
      }
      mutations.add(
          Mutation.newInsertBuilder("knowledge.business_rules")
              .set("rule_id")
              .to(UUID.randomUUID().toString())
              .set("project_id")
              .to(source.projectId())
              .set("rule_name")
              .to(firstNonBlank(rule.getRuleName(), "unnamed_rule"))
              .set("rule_type")
              .to(firstNonBlank(rule.getRuleType(), "unknown"))
              .set("condition_text")
              .to(rule.getCondition())
              .set("outcome_text")
              .to(rule.getOutcome())
              .set("source_business_component_name")
              .to(rule.getSourceBusinessComponentName())
              .set("priority")
              .to(rule.getPriority())
              .set("source_artifact_id")
              .to(artifactId)
              .set("source_chunk_id")
              .to(chunkId)
              .set("confidence")
              .to(rule.getConfidence())
              .set("created_at")
              .to(Timestamp.now())
              .set("updated_at")
              .to(Timestamp.now())
              .build());
    }
    return mutations;
  }

  private List<Mutation> businessFlowMutations(
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      KnowledgeExtractionResult extraction) {
    if (extraction.getBusinessFlows() == null) {
      return List.of();
    }
    List<Mutation> mutations = new ArrayList<>();
    for (BusinessFlow flow : extraction.getBusinessFlows()) {
      if (isBlank(flow.getFlowName())) {
        continue;
      }
      String flowId = UUID.randomUUID().toString();
      mutations.add(
          Mutation.newInsertBuilder("knowledge.business_flows")
              .set("flow_id")
              .to(flowId)
              .set("project_id")
              .to(source.projectId())
              .set("flow_name")
              .to(flow.getFlowName())
              .set("trigger_text")
              .to(flow.getTrigger())
              .set("outcome_text")
              .to(flow.getOutcome())
              .set("owner")
              .to(flow.getOwner())
              .set("source_artifact_id")
              .to(artifactId)
              .set("source_chunk_id")
              .to(chunkId)
              .set("confidence")
              .to(flow.getConfidence())
              .set("created_at")
              .to(Timestamp.now())
              .set("updated_at")
              .to(Timestamp.now())
              .build());
      if (flow.getSteps() != null) {
        for (BusinessFlowStep step : flow.getSteps()) {
          mutations.add(flowStepMutation(flowId, step));
        }
      }
    }
    return mutations;
  }

  private Mutation flowStepMutation(String flowId, BusinessFlowStep step) {
    return Mutation.newInsertBuilder("knowledge.business_flow_steps")
        .set("step_id")
        .to(UUID.randomUUID().toString())
        .set("flow_id")
        .to(flowId)
        .set("sequence_number")
        .to(step.getSequence())
        .set("step_name")
        .to(step.getStepName())
        .set("actor")
        .to(step.getActor())
        .set("action_text")
        .to(step.getAction())
        .set("input_text")
        .to(step.getInput())
        .set("output_text")
        .to(step.getOutput())
        .set("next_step")
        .to(step.getNextStep())
        .set("created_at")
        .to(Timestamp.now())
        .build();
  }

  private List<Mutation> deploymentResourceMutations(
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      SemanticChunk chunk,
      KnowledgeExtractionResult extraction,
      Map<String, String> resourceIds) {
    if (extraction.getDeploymentResources() == null) {
      return List.of();
    }
    List<Mutation> mutations = new ArrayList<>();
    for (DeploymentResource resource : extraction.getDeploymentResources()) {
      if (isBlank(resource.getResourceName())) {
        continue;
      }
      String resourceId =
          resourceIds.computeIfAbsent(
              resourceKey(
                  resource.getResourceName(),
                  resource.getResourceType(),
                  resource.getProvider(),
                  resource.getEnvironment()),
              ignored -> UUID.randomUUID().toString());
      ClassificationResult classification = chunk.getClassification();
      mutations.add(
          Mutation.newInsertOrUpdateBuilder("knowledge.deployment_resources")
              .set("resource_id")
              .to(resourceId)
              .set("project_id")
              .to(source.projectId())
              .set("domain")
              .to(domain(classification))
              .set("subdomain")
              .to(subdomain(classification))
              .set("resource_name")
              .to(resource.getResourceName())
              .set("resource_type")
              .to(firstNonBlank(resource.getResourceType(), "unknown"))
              .set("provider")
              .to(firstNonBlank(resource.getProvider(), "unknown"))
              .set("hosting_model")
              .to(firstNonBlank(resource.getHostingModel(), "unknown"))
              .set("environment")
              .to(firstNonBlank(resource.getEnvironment(), "unspecified"))
              .set("region")
              .to(resource.getRegion())
              .set("criticality")
              .to(resource.getCriticality())
              .set("lifecycle")
              .to(firstNonBlank(resource.getLifecycle(), "unknown"))
              .set("source_artifact_id")
              .to(artifactId)
              .set("source_chunk_id")
              .to(chunkId)
              .set("confidence")
              .to(resource.getConfidence())
              .set("created_at")
              .to(Timestamp.now())
              .set("updated_at")
              .to(Timestamp.now())
              .build());
      mutations.addAll(resourceConfigMutations(resourceId, chunkId, resource.getConfigs()));
    }
    return mutations;
  }

  private List<Mutation> relationshipMutations(
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      KnowledgeExtractionResult extraction,
      Map<String, String> componentIds,
      Map<String, String> resourceIds) {
    if (extraction.getRelationships() == null) {
      return List.of();
    }
    List<Mutation> mutations = new ArrayList<>();
    for (KnowledgeRelationship relationship : extraction.getRelationships()) {
      if (isBlank(relationship.getSourceName()) || isBlank(relationship.getTargetName())) {
        continue;
      }
      mutations.add(
          Mutation.newInsertBuilder("knowledge.knowledge_relationships")
              .set("relationship_id")
              .to(UUID.randomUUID().toString())
              .set("project_id")
              .to(source.projectId())
              .set("source_name")
              .to(relationship.getSourceName())
              .set("source_type")
              .to(firstNonBlank(relationship.getSourceType(), "unknown"))
              .set("source_ref_id")
              .to(
                  resolveRefId(
                      relationship.getSourceType(),
                      relationship.getSourceName(),
                      componentIds,
                      resourceIds))
              .set("target_name")
              .to(relationship.getTargetName())
              .set("target_type")
              .to(firstNonBlank(relationship.getTargetType(), "unknown"))
              .set("target_ref_id")
              .to(
                  resolveRefId(
                      relationship.getTargetType(),
                      relationship.getTargetName(),
                      componentIds,
                      resourceIds))
              .set("relationship_type")
              .to(firstNonBlank(relationship.getRelationshipType(), "depends_on"))
              .set("context")
              .to(relationship.getContext())
              .set("source_artifact_id")
              .to(artifactId)
              .set("source_chunk_id")
              .to(chunkId)
              .set("confidence")
              .to(relationship.getConfidence())
              .set("created_at")
              .to(Timestamp.now())
              .build());
    }
    return mutations;
  }

  private List<Mutation> usageProfileMutations(
      SourceDocumentMetadata source, String chunkId, KnowledgeExtractionResult extraction) {
    if (extraction.getUsageProfiles() == null) {
      return List.of();
    }
    List<Mutation> mutations = new ArrayList<>();
    for (UsageProfile usageProfile : extraction.getUsageProfiles()) {
      mutations.add(
          Mutation.newInsertBuilder("knowledge.usage_profiles")
              .set("profile_id")
              .to(UUID.randomUUID().toString())
              .set("project_id")
              .to(source.projectId())
              .set("environment")
              .to(firstNonBlank(usageProfile.getEnvironment(), "unspecified"))
              .set("users_count")
              .to(usageProfile.getUsersCount())
              .set("requests_per_day")
              .to(usageProfile.getRequestsPerDay())
              .set("peak_rps")
              .to(usageProfile.getPeakRps())
              .set("data_ingest_gb_per_day")
              .to(usageProfile.getDataIngestGbPerDay())
              .set("data_retention_days")
              .to(usageProfile.getDataRetentionDays())
              .set("storage_growth_gb_per_month")
              .to(usageProfile.getStorageGrowthGbPerMonth())
              .set("availability_target")
              .to(usageProfile.getAvailabilityTarget())
              .set("dr_required")
              .to(usageProfile.getDrRequired())
              .set("source_chunk_id")
              .to(chunkId)
              .set("created_at")
              .to(Timestamp.now())
              .set("updated_at")
              .to(Timestamp.now())
              .build());
    }
    return mutations;
  }

  private List<Mutation> costEstimateMutations(
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      Map<String, String> resourceIds,
      KnowledgeExtractionResult extraction) {
    if (extraction.getCostEstimates() == null) {
      return List.of();
    }
    List<Mutation> mutations = new ArrayList<>();
    for (ResourceCostEstimate estimate : extraction.getCostEstimates()) {
      mutations.add(
          Mutation.newInsertBuilder("knowledge.resource_cost_estimates")
              .set("estimate_id")
              .to(UUID.randomUUID().toString())
              .set("project_id")
              .to(source.projectId())
              .set("resource_id")
              .to(findIdByName(resourceIds, estimate.getResourceName()))
              .set("resource_name")
              .to(estimate.getResourceName())
              .set("environment")
              .to(firstNonBlank(estimate.getEnvironment(), "unspecified"))
              .set("provider")
              .to(firstNonBlank(estimate.getProvider(), "unknown"))
              .set("billing_model")
              .to(firstNonBlank(estimate.getBillingModel(), "unknown"))
              .set("quantity")
              .to(estimate.getQuantity())
              .set("unit")
              .to(estimate.getUnit())
              .set("unit_cost")
              .to(estimate.getUnitCost())
              .set("estimated_monthly_cost")
              .to(estimate.getEstimatedMonthlyCost())
              .set("currency")
              .to(firstNonBlank(estimate.getCurrency(), "USD"))
              .set("pricing_source")
              .to(estimate.getPricingSource())
              .set("pricing_date")
              .to(parseDate(estimate.getPricingDate()))
              .set("assumptions")
              .to(estimate.getAssumptions())
              .set("source_artifact_id")
              .to(artifactId)
              .set("source_chunk_id")
              .to(chunkId)
              .set("confidence")
              .to(estimate.getConfidence())
              .set("created_at")
              .to(Timestamp.now())
              .set("updated_at")
              .to(Timestamp.now())
              .build());
    }
    return mutations;
  }

  private void addNote(
      List<Mutation> mutations,
      SourceDocumentMetadata source,
      String artifactId,
      String chunkId,
      String noteType,
      String noteText) {
    if (isBlank(noteText)) {
      return;
    }
    mutations.add(
        Mutation.newInsertBuilder("knowledge.knowledge_notes")
            .set("note_id")
            .to(UUID.randomUUID().toString())
            .set("project_id")
            .to(source.projectId())
            .set("note_type")
            .to(noteType)
            .set("note_text")
            .to(noteText)
            .set("source_artifact_id")
            .to(artifactId)
            .set("source_chunk_id")
            .to(chunkId)
            .set("created_at")
            .to(Timestamp.now())
            .build());
  }

  private Mutation documentMutation(
      String documentId, String artifactId, SourceDocumentMetadata source, int chunkCount) {
    return Mutation.newInsertBuilder("knowledge.ingestion_documents")
        .set("document_id")
        .to(documentId)
        .set("artifact_id")
        .to(artifactId)
        .set("project_id")
        .to(source.projectId())
        .set("source_bucket")
        .to(source.bucketName())
        .set("source_object")
        .to(source.objectName())
        .set("source_generation")
        .to(source.generation())
        .set("source_checksum")
        .to(source.checksum())
        .set("content_hash")
        .to(source.contentHash())
        .set("chunk_count")
        .to(chunkCount)
        .set("created_at")
        .to(Timestamp.now())
        .build();
  }

  private Mutation chunkMutation(
      String chunkId,
      String documentId,
      String artifactId,
      SourceDocumentMetadata source,
      SemanticChunk chunk) {
    ClassificationResult result = chunk.getClassification();
    Mutation.WriteBuilder builder =
        Mutation.newInsertBuilder("knowledge.semantic_chunks")
            .set("chunk_id")
            .to(chunkId)
            .set("document_id")
            .to(documentId)
            .set("artifact_id")
            .to(artifactId)
            .set("project_id")
            .to(source.projectId())
            .set("source_bucket")
            .to(source.bucketName())
            .set("source_object")
            .to(source.objectName())
            .set("source_generation")
            .to(source.generation())
            .set("source_checksum")
            .to(source.checksum())
            .set("document_content_hash")
            .to(source.contentHash())
            .set("chunk_index")
            .to(chunk.getChunkIndex())
            .set("total_chunks")
            .to(chunk.getTotalChunks())
            .set("char_start")
            .to(chunk.getCharStart())
            .set("char_end")
            .to(chunk.getCharEnd())
            .set("chunk_content_hash")
            .to(chunk.getContentHash())
            .set("domain")
            .to(domain(result))
            .set("subdomain")
            .to(subdomain(result))
            .set("content")
            .to(chunk.getContent())
            .set("created_at")
            .to(Timestamp.now());

    List<Double> embedding = chunk.getEmbedding();
    if (embedding != null && !embedding.isEmpty()) {
      builder
          .set("embedding")
          .toFloat64Array(embedding.stream().mapToDouble(Double::doubleValue).toArray());
    }

    return builder.build();
  }

  private Mutation artifactMutation(
      String artifactId, SourceDocumentMetadata source, SemanticChunk chunk) {
    ClassificationResult result = chunk.getClassification();
    return Mutation.newInsertBuilder("knowledge.artifacts")
        .set("artifact_id")
        .to(artifactId)
        .set("project_id")
        .to(source.projectId())
        .set("domain")
        .to(domain(result))
        .set("subdomain")
        .to(subdomain(result))
        .set("source_bucket")
        .to(source.bucketName())
        .set("source_object")
        .to(source.objectName())
        .set("source_generation")
        .to(source.generation())
        .set("source_checksum")
        .to(source.checksum())
        .set("content_hash")
        .to(source.contentHash())
        .set("artifact_type")
        .to(source.artifactType())
        .set("file_type")
        .to(source.fileType())
        .set("title")
        .to(source.title())
        .set("is_current")
        .to(true)
        .set("created_at")
        .to(Timestamp.now())
        .set("updated_at")
        .to(Timestamp.now())
        .build();
  }

  private Mutation projectMutation(SourceDocumentMetadata source) {
    return Mutation.newInsertOrUpdateBuilder("knowledge.projects")
        .set("project_id")
        .to(source.projectId())
        .set("project_name")
        .to(source.projectId())
        .set("source_bucket")
        .to(source.bucketName())
        .set("gcs_prefix")
        .to(source.projectId() + "/")
        .set("created_at")
        .to(Timestamp.now())
        .set("updated_at")
        .to(Timestamp.now())
        .build();
  }

  private Mutation domainMutation(SourceDocumentMetadata source, SemanticChunk chunk) {
    ClassificationResult result = chunk.getClassification();
    return Mutation.newInsertOrUpdateBuilder("knowledge.domains")
        .set("project_id")
        .to(source.projectId())
        .set("domain")
        .to(domain(result))
        .set("created_at")
        .to(Timestamp.now())
        .set("updated_at")
        .to(Timestamp.now())
        .build();
  }

  private Mutation subdomainMutation(SourceDocumentMetadata source, SemanticChunk chunk) {
    ClassificationResult result = chunk.getClassification();
    return Mutation.newInsertOrUpdateBuilder("knowledge.subdomains")
        .set("project_id")
        .to(source.projectId())
        .set("domain")
        .to(domain(result))
        .set("subdomain")
        .to(subdomain(result))
        .set("created_at")
        .to(Timestamp.now())
        .set("updated_at")
        .to(Timestamp.now())
        .build();
  }

  private List<Mutation> resourceConfigMutations(
      String resourceId, String chunkId, List<ResourceConfig> configs) {
    if (configs == null) {
      return List.of();
    }
    List<Mutation> mutations = new ArrayList<>();
    for (ResourceConfig config : configs) {
      if (isBlank(config.getKey()) || isBlank(config.getValue())) {
        continue;
      }
      mutations.add(
          Mutation.newInsertBuilder("knowledge.deployment_resource_configs")
              .set("config_id")
              .to(UUID.randomUUID().toString())
              .set("resource_id")
              .to(resourceId)
              .set("config_key")
              .to(config.getKey())
              .set("config_value")
              .to(config.getValue())
              .set("unit")
              .to(config.getUnit())
              .set("source_chunk_id")
              .to(chunkId)
              .set("created_at")
              .to(Timestamp.now())
              .build());
    }
    return mutations;
  }

  private String resolveRefId(
      String type, String name, Map<String, String> componentIds, Map<String, String> resourceIds) {
    String normalizedType = normalized(type);
    if (normalizedType.contains("resource")
        || normalizedType.contains("cloud")
        || normalizedType.contains("on_prem")) {
      return findIdByName(resourceIds, name);
    }
    if (normalizedType.contains("component")
        || normalizedType.contains("business")
        || normalizedType.contains("technical")) {
      return findIdByName(componentIds, name);
    }
    return firstNonBlank(findIdByName(componentIds, name), findIdByName(resourceIds, name));
  }

  private String findIdByName(Map<String, String> ids, String name) {
    if (isBlank(name)) {
      return null;
    }
    String normalizedName = normalized(name);
    for (Map.Entry<String, String> entry : ids.entrySet()) {
      if (entry.getKey().contains("|" + normalizedName + "|")) {
        return entry.getValue();
      }
    }
    return null;
  }

  private String componentKey(String layer, String name, String type) {
    return "component|" + normalized(layer) + "|" + normalized(name) + "|" + normalized(type);
  }

  private String resourceKey(String name, String type, String provider, String environment) {
    return "resource|"
        + normalized(name)
        + "|"
        + normalized(type)
        + "|"
        + normalized(provider)
        + "|"
        + normalized(environment);
  }

  private Date parseDate(String value) {
    if (isBlank(value)) {
      return null;
    }
    try {
      return Date.parseDate(value);
    } catch (Exception ignored) {
      return null;
    }
  }

  private String domain(ClassificationResult result) {
    return result == null ? "Unclassified" : firstNonBlank(result.getDomain(), "Unclassified");
  }

  private String subdomain(ClassificationResult result) {
    return result == null ? "General" : firstNonBlank(result.getSubdomain(), "General");
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private String normalized(String value) {
    return firstNonBlank(value, "unknown").trim().toLowerCase();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
