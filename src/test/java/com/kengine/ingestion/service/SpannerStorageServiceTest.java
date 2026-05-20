package com.kengine.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Mutation;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpannerStorageServiceTest {

  @Mock private DatabaseClient databaseClient;

  @InjectMocks private SpannerStorageService storageService;

  @Test
  void savesSolutionKnowledgeDocument() {
    String content = "test content";
    ClassificationResult result = new ClassificationResult();
    result.setDomain("MDM");
    result.setSubdomain("Customer");
    SemanticChunk chunk =
        SemanticChunk.builder()
            .chunkIndex(0)
            .totalChunks(1)
            .charStart(0)
            .charEnd(content.length())
            .content(content)
            .contentHash("chunk-hash")
            .classification(result)
            .embedding(List.of(0.1, 0.2))
            .knowledgeExtraction(knowledgeExtraction())
            .build();
    SourceDocumentMetadata source =
        new SourceDocumentMetadata(
            "project-a",
            "bucket",
            "project-a/object.txt",
            123L,
            "crc32c",
            "document-hash",
            "text_doc",
            "txt",
            "object.txt");

    storageService.saveDocument(source, List.of(chunk));

    ArgumentCaptor<List<Mutation>> captor = ArgumentCaptor.forClass(List.class);
    verify(databaseClient).write(captor.capture());

    List<Mutation> mutations = captor.getValue();
    assertEquals(20, mutations.size());
  }

  private KnowledgeExtractionResult knowledgeExtraction() {
    BusinessComponent businessComponent = new BusinessComponent();
    businessComponent.setComponentName("Customer Onboarding");
    businessComponent.setComponentType("capability");
    businessComponent.setCapability("Create customer profile");
    businessComponent.setLifecycle("existing");

    TechnicalComponent technicalComponent = new TechnicalComponent();
    technicalComponent.setComponentName("Customer API");
    technicalComponent.setComponentType("api");
    technicalComponent.setResponsibility("Accepts customer requests");
    technicalComponent.setTechnology("Spring Boot");
    technicalComponent.setLifecycle("proposed");

    BusinessRule rule = new BusinessRule();
    rule.setRuleName("Validate customer status");
    rule.setRuleType("validation");
    rule.setCondition("Customer status must be active");
    rule.setOutcome("Reject inactive customers");

    BusinessFlowStep step = new BusinessFlowStep();
    step.setSequence(1);
    step.setStepName("Validate input");
    step.setActor("Customer API");
    step.setAction("Validate request");

    BusinessFlow flow = new BusinessFlow();
    flow.setFlowName("Create Customer");
    flow.setTrigger("Create request received");
    flow.setOutcome("Customer profile created");
    flow.setSteps(List.of(step));

    ResourceConfig config = new ResourceConfig();
    config.setKey("partitions");
    config.setValue("12");

    DeploymentResource resource = new DeploymentResource();
    resource.setResourceName("customer-events");
    resource.setResourceType("kafka_topic");
    resource.setProvider("on_prem");
    resource.setHostingModel("on_prem");
    resource.setEnvironment("prod");
    resource.setLifecycle("existing");
    resource.setConfigs(List.of(config));

    KnowledgeRelationship relationship = new KnowledgeRelationship();
    relationship.setSourceName("Customer API");
    relationship.setSourceType("technical_component");
    relationship.setTargetName("customer-events");
    relationship.setTargetType("deployment_resource");
    relationship.setRelationshipType("publishes_to");

    UsageProfile usageProfile = new UsageProfile();
    usageProfile.setEnvironment("prod");
    usageProfile.setPeakRps(25.0);

    ResourceCostEstimate estimate = new ResourceCostEstimate();
    estimate.setResourceName("customer-events");
    estimate.setEnvironment("prod");
    estimate.setProvider("on_prem");
    estimate.setBillingModel("monthly");
    estimate.setEstimatedMonthlyCost(250.0);
    estimate.setCurrency("USD");

    KnowledgeExtractionResult result = new KnowledgeExtractionResult();
    result.setArchitecturalSummary("Customer capability uses API and events.");
    result.setBusinessComponents(List.of(businessComponent));
    result.setTechnicalComponents(List.of(technicalComponent));
    result.setBusinessRules(List.of(rule));
    result.setBusinessFlows(List.of(flow));
    result.setDeploymentResources(List.of(resource));
    result.setRelationships(List.of(relationship));
    result.setUsageProfiles(List.of(usageProfile));
    result.setCostEstimates(List.of(estimate));
    result.setMigrationNotes(List.of("Replace legacy rule with service validation."));
    return result;
  }
}
