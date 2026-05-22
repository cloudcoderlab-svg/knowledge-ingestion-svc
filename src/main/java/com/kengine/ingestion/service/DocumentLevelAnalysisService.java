package com.kengine.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kengine.ingestion.dto.DocumentContent;
import com.kengine.ingestion.dto.DocumentKnowledge;
import com.kengine.ingestion.dto.SourceDocumentMetadata;
import com.kengine.ingestion.helper.JsonResponseExtractor;
import com.kengine.ingestion.helper.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Performs document-level analysis before chunking. This service analyzes the entire document to
 * extract high-level architectural context, domain classification, and major components/patterns
 * that will inform chunk-level extraction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentLevelAnalysisService {

  private final VertexAIService vertexAIService;
  private final PromptLoader promptLoader;
  private final XMLPlatformDetector platformDetector;
  private final ObjectMapper mapper;

  @Value("${ingestion.extraction.enable-document-level-analysis:true}")
  private boolean enableDocumentLevelAnalysis;

  @Value("${ingestion.parser.max-document-size-mb:50}")
  private int maxDocumentSizeMb;

  /**
   * Analyzes the full document content to extract high-level knowledge. This runs BEFORE chunking
   * to preserve document-level context.
   *
   * @param content the document content (text, diagrams, tables)
   * @param source the source document metadata
   * @return DocumentKnowledge containing overall architecture, domain, components, etc.
   */
  public DocumentKnowledge analyze(DocumentContent content, SourceDocumentMetadata source) {
    if (!enableDocumentLevelAnalysis) {
      log.info(
          "Document-level analysis is disabled. Skipping for artifact: {}", source.objectName());
      return createEmptyDocumentKnowledge();
    }

    try {
      log.info("Starting document-level analysis for artifact: {}", source.objectName());

      // Get full document text
      String fullText = buildFullDocumentText(content);

      // Check document size limit
      if (isDocumentTooLarge(fullText)) {
        log.warn(
            "Document too large for full analysis ({}MB limit). Using truncated analysis.",
            maxDocumentSizeMb);
        fullText = truncateDocument(fullText);
      }

      // Detect XML platform type for specialized extraction
      XMLPlatformDetector.XMLPlatform platform = detectPlatformFromFileType(source, fullText);
      log.info("Detected platform: {} for artifact: {}", platform, source.objectName());

      // Load platform-specific or generic prompt
      String promptFile = getPromptFileForPlatform(platform);
      String template = promptLoader.load(promptFile);
      String prompt = template.replace("{{DOCUMENT_CONTENT}}", fullText);

      log.info("Using prompt file: {} for platform: {}", promptFile, platform);

      // Call Vertex AI with the full document
      log.debug("Calling Vertex AI for document-level analysis");
      String response = vertexAIService.generate(prompt);

      // Parse JSON response into DocumentKnowledge
      DocumentKnowledge knowledge =
          mapper.readValue(JsonResponseExtractor.object(response), DocumentKnowledge.class);

      // Store detected platform for downstream use
      knowledge.setDetectedPlatform(platform.name());

      log.info(
          "Document-level analysis completed. Platform: {}, Domain: {}, Components: {}, APIs: {}",
          platform,
          knowledge.getDomain(),
          knowledge.getIdentifiedComponents() != null
              ? knowledge.getIdentifiedComponents().size()
              : 0,
          knowledge.getIdentifiedAPIs() != null ? knowledge.getIdentifiedAPIs().size() : 0);

      return knowledge;

    } catch (Exception e) {
      log.error("Error during document-level analysis for artifact: {}", source.objectName(), e);
      return createEmptyDocumentKnowledge();
    }
  }

  /** Builds full document text from DocumentContent including diagrams and tables. */
  private String buildFullDocumentText(DocumentContent content) {
    StringBuilder fullText = new StringBuilder();

    // Add main text content
    if (content.getTextContent() != null && !content.getTextContent().isEmpty()) {
      fullText.append(content.getTextContent());
    }

    // Add diagram descriptions
    if (content.getDiagrams() != null && !content.getDiagrams().isEmpty()) {
      fullText.append("\n\n=== DIAGRAMS ===\n");
      content
          .getDiagrams()
          .forEach(
              diagram -> {
                fullText.append("\nDiagram (Page ").append(diagram.getPageNumber()).append("): ");
                fullText.append(diagram.getDescription());
              });
    }

    // Add table data
    if (content.getTables() != null && !content.getTables().isEmpty()) {
      fullText.append("\n\n=== TABLES ===\n");
      content
          .getTables()
          .forEach(
              table -> {
                if (table.getCaption() != null && !table.getCaption().isEmpty()) {
                  fullText.append("\nTable: ").append(table.getCaption());
                }
                fullText.append("\nHeaders: ").append(String.join(" | ", table.getHeaders()));
                table
                    .getRows()
                    .forEach(
                        row -> {
                          fullText.append("\n").append(String.join(" | ", row));
                        });
                fullText.append("\n");
              });
    }

    return fullText.toString();
  }

  private boolean isDocumentTooLarge(String text) {
    // Approximate: 1 char ≈ 1 byte
    long sizeInMb = text.length() / (1024 * 1024);
    return sizeInMb > maxDocumentSizeMb;
  }

  private String truncateDocument(String text) {
    // Keep first 80% and last 20% to preserve beginning and end context
    int maxChars = maxDocumentSizeMb * 1024 * 1024;
    int keepFirst = (int) (maxChars * 0.8);
    int keepLast = (int) (maxChars * 0.2);

    if (text.length() <= maxChars) {
      return text;
    }

    String beginning = text.substring(0, keepFirst);
    String ending = text.substring(text.length() - keepLast);

    return beginning + "\n\n... [DOCUMENT TRUNCATED] ...\n\n" + ending;
  }

  private DocumentKnowledge createEmptyDocumentKnowledge() {
    return DocumentKnowledge.builder()
        .overallArchitecture("Not analyzed")
        .systemSummary("Document-level analysis was skipped or failed")
        .domain("Unknown")
        .subdomain("Unknown")
        .keyPatterns(java.util.Collections.emptyList())
        .technologies(java.util.Collections.emptyList())
        .identifiedComponents(java.util.Collections.emptyList())
        .identifiedAPIs(java.util.Collections.emptyList())
        .identifiedWorkflows(java.util.Collections.emptyList())
        .build();
  }

  /**
   * Detects platform type based on file type and content. Only XML files are analyzed for
   * platform-specific detection.
   */
  private XMLPlatformDetector.XMLPlatform detectPlatformFromFileType(
      SourceDocumentMetadata source, String content) {
    // Only detect platform for XML files
    if ("xml".equalsIgnoreCase(source.fileType())) {
      return platformDetector.detectPlatform(content);
    }
    // For non-XML files, use generic extraction
    return XMLPlatformDetector.XMLPlatform.GENERIC_XML;
  }

  /**
   * Gets the appropriate prompt file for the detected platform. Falls back to generic prompt if
   * platform-specific prompt doesn't exist.
   */
  private String getPromptFileForPlatform(XMLPlatformDetector.XMLPlatform platform) {
    // For generic XML or if platform-specific prompt doesn't exist yet,
    // use the generic document-level analysis prompt
    if (platform == XMLPlatformDetector.XMLPlatform.GENERIC_XML) {
      return "prompt/document-level-analysis-prompt.txt";
    }

    // Try to load platform-specific prompt, fall back to generic if not found
    String platformPromptFile = "prompt/" + platformDetector.getPromptFileName(platform);

    // For now, return generic prompt for all platforms
    // Platform-specific prompts will be created incrementally
    // TODO: Create platform-specific document-level analysis prompts
    log.debug(
        "Platform-specific prompt {} not yet implemented, using generic prompt",
        platformPromptFile);
    return "prompt/document-level-analysis-prompt.txt";
  }
}
