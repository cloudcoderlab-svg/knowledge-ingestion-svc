package com.kengine.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.kengine.ingestion.dto.ClassificationResult;
import com.kengine.ingestion.dto.SemanticChunk;
import com.kengine.ingestion.dto.SourceDocumentMetadata;
import com.kengine.ingestion.parser.DocumentParser;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionServiceTest {

  @Mock private DocumentParser genericParser;
  @Mock private SemanticClassificationService classificationService;
  @Mock private KnowledgeExtractionService knowledgeExtractionService;
  @Mock private EmbeddingService embeddingService;
  @Mock private PostgresStorageService storageService;
  @Mock private DocumentChunker documentChunker;
  @Mock private Storage storage;

  @InjectMocks private KnowledgeIngestionService ingestionService;

  @Test
  void testIngestFromGcs() throws Exception {
    // Arrange
    String bucket = "test-bucket";
    String object = "project-a/test-file.txt";
    String content = "test content";
    ClassificationResult result = new ClassificationResult();
    result.setDomain("MDM");

    Blob blob = mock(Blob.class);
    ReadChannel readChannel = mock(ReadChannel.class);

    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.reader()).thenReturn(readChannel);
    when(blob.getGeneration()).thenReturn(123L);
    when(blob.getCrc32c()).thenReturn("crc32c");

    SemanticChunk chunk =
        SemanticChunk.builder()
            .chunkIndex(0)
            .totalChunks(1)
            .charStart(0)
            .charEnd(content.length())
            .content(content)
            .contentHash("chunk-hash")
            .build();
    when(genericParser.parse(any(InputStream.class))).thenReturn(content);
    when(documentChunker.contentHash(content)).thenReturn("document-hash");
    when(storageService.findExistingClassification(any(SourceDocumentMetadata.class)))
        .thenReturn(null);
    when(documentChunker.chunk(content)).thenReturn(List.of(chunk));
    when(classificationService.classify(content)).thenReturn(result);
    when(embeddingService.embedding(content)).thenReturn(List.of(0.1, 0.2));

    // Act
    ClassificationResult actual = ingestionService.ingestFromGcs(bucket, object);

    // Assert
    assertEquals("MDM", actual.getDomain());
    verify(storageService).saveDocument(any(SourceDocumentMetadata.class), any());
    verify(knowledgeExtractionService).extract(content);
  }

  @Test
  void marksXmlArtifacts() throws Exception {
    SourceDocumentMetadata source =
        ingestAndCaptureSource("project-a/workflow/customer-workflow.xml");

    assertEquals("xml_doc", source.artifactType());
    assertEquals("xml", source.fileType());
  }

  @Test
  void marksGenericXmlArtifacts() throws Exception {
    SourceDocumentMetadata source =
        ingestAndCaptureSource("project-a/config/service-definition.xml");

    assertEquals("xml_doc", source.artifactType());
    assertEquals("xml", source.fileType());
  }

  @Test
  void skipsAlreadyIngestedContent() throws Exception {
    String bucket = "test-bucket";
    String object = "project-a/test-file.txt";
    String content = "test content";
    ClassificationResult existing = new ClassificationResult();
    existing.setDomain("MDM");

    Blob blob = mock(Blob.class);
    ReadChannel readChannel = mock(ReadChannel.class);
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.reader()).thenReturn(readChannel);
    when(genericParser.parse(any(InputStream.class))).thenReturn(content);
    when(documentChunker.contentHash(content)).thenReturn("document-hash");
    when(storageService.findExistingClassification(any(SourceDocumentMetadata.class)))
        .thenReturn(existing);

    ClassificationResult actual = ingestionService.ingestFromGcs(bucket, object);

    assertEquals("MDM", actual.getDomain());
    verifyNoInteractions(classificationService, embeddingService, knowledgeExtractionService);
    verify(storageService, never()).saveDocument(any(), any());
  }

  @Test
  void rejectsRootLevelGcsObjects() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> ingestionService.ingestFromGcs("test-bucket", "test-file.txt"));

    assertEquals(
        "GCS object must be stored under a project directory: test-file.txt",
        exception.getMessage());
    verifyNoInteractions(storage);
  }

  private SourceDocumentMetadata ingestAndCaptureSource(String objectName) throws Exception {
    String content = "test content";
    ClassificationResult result = new ClassificationResult();
    result.setDomain("MDM");

    Blob blob = mock(Blob.class);
    ReadChannel readChannel = mock(ReadChannel.class);
    when(storage.get(any(BlobId.class))).thenReturn(blob);
    when(blob.reader()).thenReturn(readChannel);
    when(blob.getGeneration()).thenReturn(123L);
    when(blob.getCrc32c()).thenReturn("crc32c");

    SemanticChunk chunk =
        SemanticChunk.builder()
            .chunkIndex(0)
            .totalChunks(1)
            .charStart(0)
            .charEnd(content.length())
            .content(content)
            .contentHash("chunk-hash")
            .build();
    when(genericParser.parse(any(InputStream.class))).thenReturn(content);
    when(documentChunker.contentHash(content)).thenReturn("document-hash");
    when(storageService.findExistingClassification(any(SourceDocumentMetadata.class)))
        .thenReturn(null);
    when(documentChunker.chunk(content)).thenReturn(List.of(chunk));
    when(classificationService.classify(content)).thenReturn(result);
    when(embeddingService.embedding(content)).thenReturn(List.of(0.1, 0.2));

    ingestionService.ingestFromGcs("test-bucket", objectName);

    ArgumentCaptor<SourceDocumentMetadata> sourceCaptor =
        ArgumentCaptor.forClass(SourceDocumentMetadata.class);
    verify(storageService).saveDocument(sourceCaptor.capture(), any());
    return sourceCaptor.getValue();
  }
}
