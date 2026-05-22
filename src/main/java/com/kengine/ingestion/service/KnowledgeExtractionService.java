package com.kengine.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kengine.ingestion.dto.KnowledgeExtractionResult;
import com.kengine.ingestion.util.JsonResponseExtractor;
import com.kengine.ingestion.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeExtractionService {

  private final VertexAIClient vertexAIClient;
  private final PromptLoader promptLoader;
  private final ObjectMapper mapper;

  public KnowledgeExtractionResult extract(String content) {
    try {
      String template = promptLoader.load("prompt/knowledge-extraction-prompt.txt");
      String prompt = template.replace("{{CONTENT}}", content);
      String response = vertexAIClient.generate(prompt);
      return mapper.readValue(
          JsonResponseExtractor.object(response), KnowledgeExtractionResult.class);
    } catch (Exception e) {
      log.error("Error extracting migration knowledge", e);
      return new KnowledgeExtractionResult();
    }
  }
}
