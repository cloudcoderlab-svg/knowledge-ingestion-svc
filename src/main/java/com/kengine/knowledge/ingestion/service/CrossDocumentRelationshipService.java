package com.kengine.knowledge.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kengine.knowledge.dto.*;
import com.kengine.knowledge.entity.*;
import com.kengine.knowledge.ingestion.service.ai.VertexAIService;
import com.kengine.knowledge.ingestion.util.JsonResponseUtils;
import com.kengine.knowledge.ingestion.util.PromptLoaderUtils;
import com.kengine.knowledge.ingestion.util.StringUtils;
import com.kengine.knowledge.repository.*;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrossDocumentRelationshipService {
  private final IngestionDocumentRepository documentRepository;
  private final RelationshipRepository relationshipRepository;
  private final VertexAIService vertexAIService;
  private final PromptLoaderUtils promptLoaderUtils;
  private final ObjectMapper mapper;

  @Transactional
  public CrossDocumentAnalysisResult analyzeAndInferRelationships(
      UUID projectId, String projectName) {
    List<IngestionDocumentEntity> documents = documentRepository.findByProjectId(projectId);
    if (documents == null || documents.size() < 2) {
      return CrossDocumentAnalysisResult.builder().build();
    }

    try {
      String template = promptLoaderUtils.load("prompt/cross-document-relationship-prompt.txt");
      String prompt =
          template
              .replace("{{PROJECT_NAME}}", projectName)
              .replace("{{DOMAIN}}", "Project")
              .replace("{{SUBDOMAIN}}", "Knowledge")
              .replace("{{OVERALL_ARCHITECTURE}}", "Multi-document project analysis")
              .replace("{{DOCUMENT_SUMMARIES}}", summaries(documents));

      String response = vertexAIService.generate(prompt);
      CrossDocumentAnalysisResult result =
          mapper.readValue(JsonResponseUtils.object(response), CrossDocumentAnalysisResult.class);
      if (result.getCrossDocumentRelationships() != null) {
        result.getCrossDocumentRelationships().stream()
            .filter(rel -> rel.getConfidence() == null || rel.getConfidence() >= 0.6)
            .forEach(rel -> saveRelationship(projectId, rel));
      }
      return result;
    } catch (Exception e) {
      log.error("Cross-document analysis failed for project: {}", projectId, e);
      return CrossDocumentAnalysisResult.builder().build();
    }
  }

  private String summaries(List<IngestionDocumentEntity> documents) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < documents.size(); i++) {
      IngestionDocumentEntity doc = documents.get(i);
      builder
          .append("--- Document ")
          .append(i + 1)
          .append(": ")
          .append(StringUtils.safeString(doc.getDocumentName(), "Untitled"))
          .append(" ---\n")
          .append("Type: ")
          .append(StringUtils.safeString(doc.getDocumentType(), "Unknown"))
          .append("\nSummary: ")
          .append(StringUtils.safeString(doc.getSummary(), "No summary available"))
          .append("\nMetadata: ")
          .append(StringUtils.safeString(doc.getExtractedMetadata(), "{}"))
          .append("\n\n");
    }
    return builder.toString();
  }

  private void saveRelationship(UUID projectId, CrossDocumentRelationship relationship) {
    relationshipRepository.save(
        RelationshipEntity.builder()
            .projectId(projectId)
            .sourceName(relationship.getSourceName())
            .sourceEntityType(nullToUnknown(relationship.getSourceType()))
            .targetName(relationship.getTargetName())
            .targetEntityType(nullToUnknown(relationship.getTargetType()))
            .relationshipType(nullToUnknown(relationship.getRelationshipType()))
            .relationshipDefinition(
                relationship.getContext()
                    + " [Cross-document: "
                    + relationship.getSourceDocument()
                    + " -> "
                    + relationship.getTargetDocument()
                    + "]")
            .confidence(relationship.getConfidence())
            .build());
  }

  private String nullToUnknown(String value) {
    return value == null || value.isBlank() ? "unknown" : value;
  }
}
