package com.kengine.ingestion.parser;

import com.kengine.ingestion.dto.DocumentContent;
import java.io.InputStream;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Orchestrates document parsing by routing to the appropriate content extractor. Routes multimodal
 * file types (PDF, images, Office docs) to Gemini extractor. Routes text-only file types to Tika
 * extractor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParserOrchestrator {

  private final GeminiMultimodalExtractor geminiMultimodalExtractor;
  private final TikaContentExtractor tikaContentExtractor;

  @Value("${ingestion.parser.use-multimodal:true}")
  private boolean useMultimodal;

  @Value("${ingestion.parser.multimodal-types:pdf,png,jpg,jpeg,docx,pptx}")
  private String multimodalTypesConfig;

  private Set<String> multimodalTypes;

  /**
   * Parses a document and extracts its content using the appropriate strategy.
   *
   * @param inputStream the document input stream
   * @param fileType the file type (extension)
   * @return DocumentContent containing text, diagrams, tables, and metadata
   * @throws Exception if parsing fails
   */
  public DocumentContent parseDocument(InputStream inputStream, String fileType) throws Exception {
    if (useMultimodal && isMultimodalType(fileType)) {
      log.info("Using multimodal extraction for file type: {}", fileType);
      return geminiMultimodalExtractor.extract(inputStream, fileType);
    } else {
      log.info("Using Tika text extraction for file type: {}", fileType);
      return tikaContentExtractor.extract(inputStream, fileType);
    }
  }

  private boolean isMultimodalType(String fileType) {
    if (multimodalTypes == null) {
      multimodalTypes = Set.of(multimodalTypesConfig.toLowerCase().split(","));
    }
    return multimodalTypes.contains(fileType.toLowerCase());
  }
}
