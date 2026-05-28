package com.kengine.knowledge.ingestion.service;

import com.kengine.knowledge.entity.*;
import com.kengine.knowledge.ingestion.service.ai.EmbeddingService;
import com.kengine.knowledge.ingestion.util.EmbeddingUtils;
import com.kengine.knowledge.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KnowledgeChunkGenerationService {
  private final DomainRepository domainRepository;
  private final ModuleRepository moduleRepository;
  private final ComponentRepository componentRepository;
  private final BusinessRuleRepository businessRuleRepository;
  private final WorkflowRepository workflowRepository;
  private final RelationshipRepository relationshipRepository;
  private final KnowledgeChunkRepository knowledgeChunkRepository;
  private final EmbeddingService embeddingService;

  @Transactional
  public int refreshKnowledgeChunks(UUID projectId) {
    knowledgeChunkRepository.deleteByProjectId(projectId);
    int count = 0;
    for (DomainEntity domain : domainRepository.findByProjectId(projectId)) {
      save(projectId, "domain", "knowledge_domains", domain.getDomainId(), domainText(domain));
      count++;
    }
    for (ModuleEntity module : moduleRepository.findByProjectId(projectId)) {
      save(projectId, "module", "knowledge_modules", module.getModuleId(), moduleText(module));
      count++;
    }
    for (ComponentEntity component : componentRepository.findByProjectId(projectId)) {
      save(
          projectId,
          "component",
          "knowledge_components",
          component.getComponentId(),
          componentText(component));
      count++;
    }
    for (BusinessRuleEntity rule : businessRuleRepository.findByProjectId(projectId)) {
      save(projectId, "rule", "knowledge_business_rules", rule.getRuleId(), ruleText(rule));
      count++;
    }
    for (WorkflowEntity workflow : workflowRepository.findByProjectId(projectId)) {
      save(
          projectId,
          "workflow",
          "knowledge_workflows",
          workflow.getWorkflowId(),
          workflowText(workflow));
      count++;
    }
    for (RelationshipEntity relationship : relationshipRepository.findByProjectId(projectId)) {
      save(
          projectId,
          "relationship",
          "knowledge_relationships",
          relationship.getRelationshipId(),
          relationshipText(relationship));
      count++;
    }
    return count;
  }

  private void save(
      UUID projectId, String chunkType, String entityType, UUID entityId, String content) {
    if (content == null || content.isBlank()) {
      return;
    }
    knowledgeChunkRepository.save(
        KnowledgeChunkEntity.builder()
            .projectId(projectId)
            .chunkType(chunkType)
            .entityType(entityType)
            .entityId(entityId)
            .content(content)
            .embedding(EmbeddingUtils.embeddingToString(embeddingService.embedding(content)))
            .build());
  }

  private String domainText(DomainEntity domain) {
    return "Domain: "
        + domain.getDomainName()
        + ". "
        + text(domain.getKnowledge(), domain.getDescription());
  }

  private String moduleText(ModuleEntity module) {
    return "Module: "
        + module.getModuleName()
        + ". Type: "
        + module.getModuleType()
        + ". Responsibility: "
        + text(module.getResponsibility(), module.getKnowledge());
  }

  private String componentText(ComponentEntity component) {
    return "Component: "
        + component.getComponentName()
        + ". Type: "
        + component.getComponentType()
        + ". Category: "
        + component.getCategory()
        + ". "
        + text(component.getKnowledge(), component.getResponsibility(), component.getCapability());
  }

  private String ruleText(BusinessRuleEntity rule) {
    return "Business rule: "
        + rule.getRuleName()
        + ". Condition: "
        + rule.getConditionText()
        + ". Outcome: "
        + rule.getOutcomeText()
        + ". Validation: "
        + rule.getValidationCriteria();
  }

  private String workflowText(WorkflowEntity workflow) {
    return "Workflow: "
        + workflow.getWorkflowName()
        + ". Trigger: "
        + workflow.getTriggerText()
        + ". Outcome: "
        + workflow.getOutcomeText()
        + ". Actor: "
        + workflow.getActor();
  }

  private String relationshipText(RelationshipEntity relationship) {
    return "Relationship: "
        + relationship.getSourceName()
        + " "
        + relationship.getRelationshipType()
        + " "
        + relationship.getTargetName()
        + ". "
        + text(relationship.getRelationshipDefinition(), relationship.getBusinessDescription());
  }

  private String text(String... values) {
    StringBuilder builder = new StringBuilder();
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        if (!builder.isEmpty()) {
          builder.append(' ');
        }
        builder.append(value);
      }
    }
    return builder.toString();
  }
}
