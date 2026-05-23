package com.kengine.ingestion.service;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.kengine.ingestion.dto.*;
import com.kengine.ingestion.parser.DocumentParserOrchestrator;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIngestionService {

  // Phase 2: Enhanced multimodal parser
  private final DocumentParserOrchestrator documentParserOrchestrator;

  // Phase 3: Document-level and enhanced chunk extraction
  private final DocumentLevelAnalysisService documentLevelAnalysisService;
  private final EnhancedKnowledgeExtractionService enhancedKnowledgeExtractionService;

  // Phase 4: Knowledge graph persistence
  private final KnowledgeGraphService knowledgeGraphService;

  // Existing services
  private final SemanticClassificationService classificationService;
  private final EmbeddingService embeddingService;
  private final PostgresStorageService storageService;
  private final DocumentChunker documentChunker;
  private final Storage storage;

  public ClassificationResult ingestFromGcs(String bucketName, String objectName) throws Exception {
    log.info("Ingesting file from GCS: gs://{}/{}", bucketName, objectName);

    String projectId = projectIdFromObjectName(objectName);
    Blob blob = storage.get(BlobId.of(bucketName, objectName));
    if (blob == null) {
      throw new IllegalArgumentException("Blob not found: " + objectName);
    }

    String fileType = fileType(objectName);

    try (ReadChannel reader = blob.reader();
        InputStream is = Channels.newInputStream(reader)) {

      // Phase 2: Enhanced multimodal parsing (supports text + diagrams + tables)
      log.info("Parsing document with multimodal support. File type: {}", fileType);
      DocumentContent documentContent = documentParserOrchestrator.parseDocument(is, fileType);

      if (documentContent.getTextContent() == null
          || documentContent.getTextContent().trim().isEmpty()) {
        throw new IllegalArgumentException("Content is empty");
      }

      SourceDocumentMetadata source =
          sourceMetadata(projectId, bucketName, objectName, blob, documentContent.getTextContent());

      return processContent(source, documentContent);
    }
  }

  /**
   * Processes document content using the new two-phase extraction pipeline: 1. Document-level
   * analysis (preserves full context) 2. Enhanced chunk extraction (uses document context for
   * better linking) 3. Knowledge graph building (persists to dedicated entity tables)
   */
  private ClassificationResult processContent(
      SourceDocumentMetadata source, DocumentContent documentContent) throws Exception {

    ClassificationResult existing = storageService.findExistingClassification(source);
    if (existing != null) {
      log.info(
          "Skipping already-ingested source: gs://{}/{} hash={}",
          source.bucketName(),
          source.objectName(),
          source.contentHash());
      return existing;
    }

    // ========================================================================
    // PHASE 3 PART 1: Document-Level Analysis (before chunking)
    // ========================================================================
    log.info("Starting document-level analysis for artifact: {}", source.objectName());
    DocumentKnowledge docKnowledge = documentLevelAnalysisService.analyze(documentContent, source);
    log.info(
        "Document-level analysis complete. Domain: {}, Subdomain: {}",
        docKnowledge.getDomain(),
        docKnowledge.getSubdomain());

    // ========================================================================
    // Chunk the document
    // ========================================================================
    String fullTextContent = documentContent.getTextContent();
    List<SemanticChunk> chunks = documentChunker.chunk(fullTextContent);
    if (chunks.isEmpty()) {
      throw new IllegalArgumentException("Content is empty after chunking");
    }
    log.info("Document chunked into {} semantic chunks", chunks.size());

    // ========================================================================
    // PHASE 3 PART 2: Enhanced Chunk Extraction (with document context)
    // ========================================================================
    List<KnowledgeExtractionResult> allChunkKnowledge = new ArrayList<>();

    for (SemanticChunk chunk : chunks) {
      String chunkContent = chunk.getContent();

      // Semantic classification (unchanged)
      chunk.setClassification(classificationService.classify(chunkContent));

      // Generate chunk embedding (unchanged)
      chunk.setEmbedding(embeddingService.embedding(chunkContent));

      // NEW: Enhanced extraction with document context
      KnowledgeExtractionResult chunkKnowledge =
          enhancedKnowledgeExtractionService.extract(chunkContent, docKnowledge);
      chunk.setKnowledgeExtraction(chunkKnowledge);

      allChunkKnowledge.add(chunkKnowledge);
    }

    log.info("Enhanced chunk extraction completed for {} chunks", chunks.size());

    // ========================================================================
    // Save chunks to semantic_chunks table (unchanged)
    // ========================================================================
    String artifactId = storageService.saveDocument(source, chunks);

    // ========================================================================
    // PHASE 4: Build Knowledge Graph (NEW)
    // ========================================================================
    log.info("Building knowledge graph for artifact: {}", source.objectName());
    knowledgeGraphService.buildKnowledgeGraph(artifactId, source, docKnowledge, allChunkKnowledge);
    log.info("Knowledge graph build completed");

    // Return classification from first chunk for backward compatibility
    return chunks.get(0).getClassification();
  }

  private SourceDocumentMetadata sourceMetadata(
      String projectId, String bucketName, String objectName, Blob blob, String content) {
    String checksum = firstNonBlank(blob.getCrc32c(), blob.getMd5(), blob.getEtag());
    return new SourceDocumentMetadata(
        projectId,
        bucketName,
        objectName,
        blob.getGeneration(),
        checksum,
        documentChunker.contentHash(content),
        artifactType(objectName),
        fileType(objectName),
        title(objectName));
  }

  private String projectIdFromObjectName(String objectName) {
    // Extract project ID from path: staged/{projectId}/{date}/file or
    // processed/{projectId}/{date}/file
    String[] parts = objectName.split("/");
    if (parts.length < 3) {
      throw new IllegalArgumentException(
          "GCS object must be stored under staged/{projectId}/{date}/ or processed/{projectId}/{date}/: "
              + objectName);
    }
    return parts[1]; // Return the project ID (second segment)
  }

  private String artifactType(String objectName) {
    String lowerName = objectName.toLowerCase();
    if (lowerName.endsWith(".xml")) {
      return "xml_doc";
    }
    if (lowerName.contains("diagram") || lowerName.contains("architecture")) {
      return "architecture_diagram";
    }
    if (lowerName.contains("requirement")) {
      return "requirements_doc";
    }
    if (lowerName.contains("estimate") || lowerName.contains("cost")) {
      return "estimate_sheet";
    }
    if (lowerName.endsWith(".md") || lowerName.endsWith(".txt")) {
      return "text_doc";
    }
    return "document";
  }

  private String fileType(String objectName) {
    int lastDot = objectName.lastIndexOf('.');
    if (lastDot < 0 || lastDot == objectName.length() - 1) {
      return "unknown";
    }
    return objectName.substring(lastDot + 1).toLowerCase();
  }

  private String title(String objectName) {
    int separator = objectName.lastIndexOf('/');
    return separator < 0 ? objectName : objectName.substring(separator + 1);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
