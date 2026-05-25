package com.kengine.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kengine.ingestion.dto.CrossDocumentAnalysisResult;
import com.kengine.ingestion.dto.CrossDocumentRelationship;
import com.kengine.ingestion.entity.DocumentKnowledgeEntity;
import com.kengine.ingestion.entity.KnowledgeRelationshipEntity;
import com.kengine.ingestion.helper.JsonResponseExtractor;
import com.kengine.ingestion.helper.PromptLoader;
import com.kengine.ingestion.helper.StringUtils;
import com.kengine.ingestion.repository.DocumentKnowledgeRepository;
import com.kengine.ingestion.repository.KnowledgeRelationshipRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for analyzing and inferring relationships between documents within the same subject using
 * AI-powered cross-document analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrossDocumentRelationshipService {

  private final DocumentKnowledgeRepository documentKnowledgeRepository;
  private final KnowledgeRelationshipRepository relationshipRepository;
  private final VertexAIService vertexAIService;
  private final PromptLoader promptLoader;
  private final ObjectMapper mapper;

  /**
   * Analyzes all documents for a subject and infers cross-document relationships using AI.
   *
   * @param subjectId the subject ID to analyze
   * @param subjectName the subject name for context
   * @return the cross-document analysis result
   */
  @Transactional
  public CrossDocumentAnalysisResult analyzeAndInferRelationships(
      UUID subjectId, String subjectName) {

    log.info("Starting cross-document relationship analysis for subject: {}", subjectName);

    // Retrieve all document knowledge for this subject
    List<DocumentKnowledgeEntity> documents =
        documentKnowledgeRepository.findBySubjectId(subjectId);

    if (documents == null || documents.size() < 2) {
      log.info(
          "Skipping cross-document analysis: subject {} has {} documents (need at least 2)",
          subjectName,
          documents != null ? documents.size() : 0);
      return CrossDocumentAnalysisResult.builder().build();
    }

    log.info(
        "Found {} documents for subject {}. Performing cross-document analysis.",
        documents.size(),
        subjectName);

    try {
      // Load the cross-document relationship prompt
      String template = promptLoader.load("prompt/cross-document-relationship-prompt.txt");

      // Build document summaries section
      StringBuilder documentSummaries = new StringBuilder();
      for (int i = 0; i < documents.size(); i++) {
        DocumentKnowledgeEntity doc = documents.get(i);
        documentSummaries
            .append("--- Document ")
            .append(i + 1)
            .append(": ")
            .append(StringUtils.safeString(doc.getTitle(), "Untitled"))
            .append(" ---\n");
        documentSummaries
            .append("Type: ")
            .append(StringUtils.safeString(doc.getDocumentType(), "Unknown"))
            .append("\n");
        documentSummaries
            .append("Summary: ")
            .append(StringUtils.safeString(doc.getSummary(), "No summary available"))
            .append("\n");
        documentSummaries
            .append("Architecture: ")
            .append(StringUtils.safeString(doc.getOverallArchitecture(), "Not specified"))
            .append("\n");
        documentSummaries
            .append("Domain: ")
            .append(StringUtils.safeString(doc.getDomain(), "Unknown"))
            .append("\n");
        documentSummaries
            .append("Subdomain: ")
            .append(StringUtils.safeString(doc.getSubdomain(), "Unknown"))
            .append("\n");

        if (doc.getIdentifiedComponents() != null && doc.getIdentifiedComponents().length > 0) {
          documentSummaries
              .append("Components: ")
              .append(String.join(", ", doc.getIdentifiedComponents()))
              .append("\n");
        }

        if (doc.getIdentifiedApis() != null && doc.getIdentifiedApis().length > 0) {
          documentSummaries
              .append("APIs: ")
              .append(String.join(", ", doc.getIdentifiedApis()))
              .append("\n");
        }

        if (doc.getIdentifiedWorkflows() != null && doc.getIdentifiedWorkflows().length > 0) {
          documentSummaries
              .append("Workflows: ")
              .append(String.join(", ", doc.getIdentifiedWorkflows()))
              .append("\n");
        }

        if (doc.getIdentifiedCapabilities() != null && doc.getIdentifiedCapabilities().length > 0) {
          documentSummaries
              .append("Capabilities: ")
              .append(String.join(", ", doc.getIdentifiedCapabilities()))
              .append("\n");
        }

        if (doc.getIdentifiedRoles() != null && doc.getIdentifiedRoles().length > 0) {
          documentSummaries
              .append("Roles: ")
              .append(String.join(", ", doc.getIdentifiedRoles()))
              .append("\n");
        }

        if (doc.getIdentifiedTerms() != null && doc.getIdentifiedTerms().length > 0) {
          documentSummaries
              .append("Terms: ")
              .append(String.join(", ", doc.getIdentifiedTerms()))
              .append("\n");
        }

        if (doc.getTechnologies() != null && doc.getTechnologies().length > 0) {
          documentSummaries
              .append("Technologies: ")
              .append(String.join(", ", doc.getTechnologies()))
              .append("\n");
        }

        if (doc.getKeyConcepts() != null && doc.getKeyConcepts().length > 0) {
          documentSummaries
              .append("Key Patterns: ")
              .append(String.join(", ", doc.getKeyConcepts()))
              .append("\n");
        }

        documentSummaries.append("\n");
      }

      // Populate the prompt
      String prompt =
          template
              .replace("{{SUBJECT_NAME}}", subjectName)
              .replace(
                  "{{DOMAIN}}", StringUtils.safeString(documents.get(0).getDomain(), "Unknown"))
              .replace(
                  "{{SUBDOMAIN}}",
                  StringUtils.safeString(documents.get(0).getSubdomain(), "Unknown"))
              .replace("{{OVERALL_ARCHITECTURE}}", "Multi-document subject analysis")
              .replace("{{DOCUMENT_SUMMARIES}}", documentSummaries.toString());

      // Call Vertex AI for analysis
      log.debug("Calling Vertex AI for cross-document relationship analysis");
      String response = vertexAIService.generate(prompt);

      // Parse JSON response
      String jsonResponse = JsonResponseExtractor.object(response);
      CrossDocumentAnalysisResult result =
          mapper.readValue(jsonResponse, CrossDocumentAnalysisResult.class);

      // Save discovered relationships
      if (result.getCrossDocumentRelationships() != null) {
        int savedCount = 0;
        for (CrossDocumentRelationship rel : result.getCrossDocumentRelationships()) {
          if (rel.getConfidence() != null && rel.getConfidence() >= 0.6) {
            saveRelationship(subjectId, rel);
            savedCount++;
          }
        }
        log.info("Saved {} cross-document relationships for subject {}", savedCount, subjectName);
      }

      // Log insights
      if (result.getCrossDocumentInsights() != null
          && result.getCrossDocumentInsights().getSharedEntities() != null) {
        log.info(
            "Identified {} shared entities across documents",
            result.getCrossDocumentInsights().getSharedEntities().size());
      }

      return result;

    } catch (Exception e) {
      log.error("Error during cross-document relationship analysis for subject {}", subjectName, e);
      return CrossDocumentAnalysisResult.builder().build();
    }
  }

  /**
   * Saves a cross-document relationship to the database.
   *
   * @param subjectId the subject ID
   * @param relationship the cross-document relationship
   */
  private void saveRelationship(UUID subjectId, CrossDocumentRelationship relationship) {
    try {
      KnowledgeRelationshipEntity entity =
          KnowledgeRelationshipEntity.builder()
              .relationshipId(UUID.randomUUID().toString())
              .subjectId(subjectId)
              .sourceName(relationship.getSourceName())
              .sourceType(relationship.getSourceType())
              .targetName(relationship.getTargetName())
              .targetType(relationship.getTargetType())
              .relationshipType(relationship.getRelationshipType())
              .context(
                  relationship.getContext()
                      + " [Cross-document: "
                      + relationship.getSourceDocument()
                      + " -> "
                      + relationship.getTargetDocument()
                      + "]")
              .confidence(relationship.getConfidence())
              .build();

      relationshipRepository.save(entity);

    } catch (Exception e) {
      log.warn(
          "Failed to save cross-document relationship: {} -> {}",
          relationship.getSourceName(),
          relationship.getTargetName(),
          e);
    }
  }
}
