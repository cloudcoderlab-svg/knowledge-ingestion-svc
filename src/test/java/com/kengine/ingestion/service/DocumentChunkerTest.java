package com.kengine.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kengine.ingestion.dto.SemanticChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

  @Test
  void chunksContentWithStableIndexesAndHashes() {
    DocumentChunker chunker = new DocumentChunker(30, 5);
    String content = "First sentence. Second sentence. Third sentence. Fourth sentence.";

    List<SemanticChunk> chunks = chunker.chunk(content);

    assertFalse(chunks.isEmpty());
    for (int i = 0; i < chunks.size(); i++) {
      SemanticChunk chunk = chunks.get(i);
      assertEquals(i, chunk.getChunkIndex());
      assertEquals(chunks.size(), chunk.getTotalChunks());
      assertFalse(chunk.getContent().isBlank());
      assertEquals(64, chunk.getContentHash().length());
    }
  }

  @Test
  void contentHashIsStableForTrimmedContent() {
    DocumentChunker chunker = new DocumentChunker(100, 10);

    assertEquals(chunker.contentHash("content"), chunker.contentHash("  content\n"));
  }

  @Test
  void returnsNoChunksForBlankContent() {
    DocumentChunker chunker = new DocumentChunker(100, 10);

    assertTrue(chunker.chunk("  \n").isEmpty());
  }
}
