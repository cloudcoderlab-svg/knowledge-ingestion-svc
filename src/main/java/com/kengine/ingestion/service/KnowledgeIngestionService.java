package com.kengine.ingestion.service;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.kengine.ingestion.dto.ClassificationResult;
import com.kengine.ingestion.dto.SemanticChunk;
import com.kengine.ingestion.dto.SourceDocumentMetadata;
import com.kengine.ingestion.parser.DocumentParser;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIngestionService {

  private final DocumentParser genericParser;
  private final SemanticClassificationService classificationService;
  private final KnowledgeExtractionService knowledgeExtractionService;
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

    try (ReadChannel reader = blob.reader();
        InputStream is = Channels.newInputStream(reader)) {

      String content = genericParser.parse(is);
      if (content == null || content.trim().isEmpty()) {
        throw new IllegalArgumentException("Content is empty");
      }
      SourceDocumentMetadata source =
          sourceMetadata(projectId, bucketName, objectName, blob, content);
      return processContent(source, content);
    }
  }

  private ClassificationResult processContent(SourceDocumentMetadata source, String content)
      throws Exception {
    ClassificationResult existing = storageService.findExistingClassification(source);
    if (existing != null) {
      log.info(
          "Skipping already-ingested source: gs://{}/{} hash={}",
          source.bucketName(),
          source.objectName(),
          source.contentHash());
      return existing;
    }

    List<SemanticChunk> chunks = documentChunker.chunk(content);
    if (chunks.isEmpty()) {
      throw new IllegalArgumentException("Content is empty after chunking");
    }

    for (SemanticChunk chunk : chunks) {
      String chunkContent = chunk.getContent();
      chunk.setClassification(classificationService.classify(chunkContent));
      chunk.setEmbedding(embeddingService.embedding(chunkContent));
      chunk.setKnowledgeExtraction(knowledgeExtractionService.extract(chunkContent));
    }

    storageService.saveDocument(source, chunks);

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
    int separator = objectName.indexOf('/');
    if (separator <= 0) {
      throw new IllegalArgumentException(
          "GCS object must be stored under a project directory: " + objectName);
    }
    return objectName.substring(0, separator);
  }

  private String artifactType(String objectName) {
    String lowerName = objectName.toLowerCase();
    if (lowerName.endsWith(".xml") && isLikelyTibcoMdmArtifact(lowerName)) {
      return "tibco_mdm_xml";
    }
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

  private boolean isLikelyTibcoMdmArtifact(String lowerName) {
    return lowerName.contains("tibco")
        || lowerName.contains("mdm")
        || lowerName.contains("rulebase")
        || lowerName.contains("workflow")
        || lowerName.contains("datamodel")
        || lowerName.contains("data-model")
        || lowerName.contains("repository");
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
