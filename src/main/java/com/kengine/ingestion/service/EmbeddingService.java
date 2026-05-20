package com.kengine.ingestion.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

  private final VertexAIClient vertexAIClient;

  public List<Double> embedding(String text) {
    return vertexAIClient.embedding(text).stream().map(Float::doubleValue).toList();
  }
}
