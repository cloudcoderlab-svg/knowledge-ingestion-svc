package com.kengine.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kengine.ingestion.dto.DocumentKnowledge;
import com.kengine.ingestion.dto.KnowledgeExtractionResult;
import com.kengine.ingestion.util.CommonUtil;
import com.kengine.ingestion.util.JsonResponseExtractor;
import com.kengine.ingestion.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Enhanced knowledge extraction service that uses document-level context to improve chunk-level
 * extraction accuracy and entity linking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedKnowledgeExtractionService {

  private final VertexAIClient vertexAIClient;
  private final PromptLoader promptLoader;
  private final XMLPlatformDetector platformDetector;
  private final ObjectMapper mapper;

  @Value("${ingestion.extraction.enable-document-level-analysis:true}")
  private boolean enableEnhancedExtraction;

  /**
   * Extracts knowledge from a chunk using document-level context. This provides better entity
   * linking and confidence scores compared to isolated chunk extraction.
   *
   * @param chunkContent the text content of the chunk
   * @param docContext document-level knowledge providing context
   * @return KnowledgeExtractionResult with enhanced entity linking
   */
  public KnowledgeExtractionResult extract(String chunkContent, DocumentKnowledge docContext) {
    if (!enableEnhancedExtraction || docContext == null) {
      log.debug("Enhanced extraction disabled or no document context. Using basic extraction.");
      return extractBasic(chunkContent);
    }

    try {
      // Get platform-specific or generic prompt based on detected platform
      String promptFile = getChunkExtractionPromptFile(docContext);
      String template = promptLoader.load(promptFile);

      log.debug(
          "Using chunk extraction prompt: {} for platform: {}",
          promptFile,
          docContext.getDetectedPlatform());

      // Populate document context placeholders
      String prompt = populatePromptWithContext(template, chunkContent, docContext);

      // Call Vertex AI
      log.debug("Calling Vertex AI for enhanced chunk extraction");
      String response = vertexAIClient.generate(prompt);

      // Parse JSON response
      KnowledgeExtractionResult result =
          mapper.readValue(JsonResponseExtractor.object(response), KnowledgeExtractionResult.class);

      log.debug(
          "Enhanced extraction completed. Rules: {}, Components: {}, Relationships: {}",
          result.getBusinessRules() != null ? result.getBusinessRules().size() : 0,
          result.getTechnicalComponents() != null ? result.getTechnicalComponents().size() : 0,
          result.getRelationships() != null ? result.getRelationships().size() : 0);

      return result;

    } catch (Exception e) {
      log.error("Error during enhanced chunk extraction. Falling back to basic extraction.", e);
      return extractBasic(chunkContent);
    }
  }

  /** Populates the prompt template with document context and chunk content. */
  private String populatePromptWithContext(
      String template, String chunkContent, DocumentKnowledge docContext) {

    String prompt = template;

    // Replace document context placeholders
    prompt =
        prompt.replace(
            "{{OVERALL_ARCHITECTURE}}",
            CommonUtil.safeString(docContext.getOverallArchitecture(), "Not specified"));
    prompt = prompt.replace("{{DOMAIN}}", CommonUtil.safeString(docContext.getDomain(), "Unknown"));
    prompt =
        prompt.replace(
            "{{SUBDOMAIN}}", CommonUtil.safeString(docContext.getSubdomain(), "Unknown"));
    prompt =
        prompt.replace(
            "{{KEY_PATTERNS}}",
            CommonUtil.safeListToString(docContext.getKeyPatterns(), "None identified"));
    prompt =
        prompt.replace(
            "{{TECHNOLOGIES}}",
            CommonUtil.safeListToString(docContext.getTechnologies(), "None identified"));
    prompt =
        prompt.replace(
            "{{IDENTIFIED_COMPONENTS}}",
            CommonUtil.safeListToString(docContext.getIdentifiedComponents(), "None identified"));
    prompt =
        prompt.replace(
            "{{IDENTIFIED_APIS}}",
            CommonUtil.safeListToString(docContext.getIdentifiedAPIs(), "None identified"));
    prompt =
        prompt.replace(
            "{{IDENTIFIED_WORKFLOWS}}",
            CommonUtil.safeListToString(docContext.getIdentifiedWorkflows(), "None identified"));

    // Replace chunk content
    prompt = prompt.replace("{{CHUNK_CONTENT}}", chunkContent);

    return prompt;
  }

  /**
   * Fallback to basic extraction without document context. Uses the original
   * knowledge-extraction-prompt.txt
   */
  private KnowledgeExtractionResult extractBasic(String content) {
    try {
      String template = promptLoader.load("prompt/knowledge-extraction-prompt.txt");
      String prompt = template.replace("{{CONTENT}}", content);
      String response = vertexAIClient.generate(prompt);
      return mapper.readValue(
          JsonResponseExtractor.object(response), KnowledgeExtractionResult.class);
    } catch (Exception e) {
      log.error("Error in basic extraction fallback", e);
      return new KnowledgeExtractionResult();
    }
  }

  /**
   * Gets the appropriate chunk extraction prompt file based on detected platform. Falls back to
   * generic enhanced extraction prompt if platform-specific prompt doesn't exist.
   */
  private String getChunkExtractionPromptFile(DocumentKnowledge docContext) {
    // If no platform detected, use generic enhanced extraction
    if (docContext.getDetectedPlatform() == null || docContext.getDetectedPlatform().isEmpty()) {
      return "prompt/enhanced-chunk-extraction-prompt.txt";
    }

    try {
      // Try to map platform to a specific prompt file
      XMLPlatformDetector.XMLPlatform platform =
          XMLPlatformDetector.XMLPlatform.valueOf(docContext.getDetectedPlatform());

      // Get platform-specific prompt file name
      String platformPromptFile = getPlatformSpecificPromptFile(platform);

      // Check if platform-specific prompt exists by attempting to load it
      // If it exists, use it; otherwise fall back to generic
      try {
        promptLoader.load(platformPromptFile);
        log.debug("Found platform-specific chunk extraction prompt: {}", platformPromptFile);
        return platformPromptFile;
      } catch (Exception e) {
        log.debug(
            "Platform-specific prompt {} not found, falling back to generic enhanced extraction",
            platformPromptFile);
        return "prompt/enhanced-chunk-extraction-prompt.txt";
      }

    } catch (IllegalArgumentException e) {
      log.warn(
          "Unknown platform type: {}, using generic extraction", docContext.getDetectedPlatform());
      return "prompt/enhanced-chunk-extraction-prompt.txt";
    }
  }

  /**
   * Maps platform enum to the chunk-level extraction prompt file name. These prompts are designed
   * for extracting knowledge from individual chunks with document-level context.
   */
  private String getPlatformSpecificPromptFile(XMLPlatformDetector.XMLPlatform platform) {
    return switch (platform) {
      case TIBCO_MDM -> "prompt/tibco-mdm-extraction-prompt.txt";
      case CAMUNDA_BPMN -> "prompt/camunda-bpmn-extraction-prompt.txt";
      case PEGA_BPM -> "prompt/pega-bpm-extraction-prompt.txt";
      case TIBCO_BPM -> "prompt/tibco-bpm-extraction-prompt.txt";
      case ACTIVITI_BPMN -> "prompt/activiti-bpmn-extraction-prompt.txt";
      case FLOWABLE_BPMN -> "prompt/flowable-bpmn-extraction-prompt.txt";
      case BPMN_20_GENERIC -> "prompt/bpmn-generic-extraction-prompt.txt";
      case INFORMATICA_MDM -> "prompt/informatica-mdm-extraction-prompt.txt";
      case SAP_MDM -> "prompt/sap-mdm-extraction-prompt.txt";
      case ORACLE_MDM -> "prompt/oracle-mdm-extraction-prompt.txt";
      case IBM_INFOSPHERE_MDM -> "prompt/ibm-infosphere-mdm-extraction-prompt.txt";
      default -> "prompt/enhanced-chunk-extraction-prompt.txt";
    };
  }
}
