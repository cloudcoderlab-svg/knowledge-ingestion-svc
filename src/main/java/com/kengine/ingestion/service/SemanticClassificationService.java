package com.kengine.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kengine.ingestion.dto.ClassificationResult;
import com.kengine.ingestion.util.JsonResponseExtractor;
import com.kengine.ingestion.util.PromptLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SemanticClassificationService {

  private final VertexAIClient vertexAIClient;
  private final PromptLoader promptLoader;
  private final ObjectMapper mapper = new ObjectMapper();

  public ClassificationResult classify(String content) throws Exception {

    String template = promptLoader.load("prompt/classification-prompt.txt");

    String prompt = template.replace("{{CONTENT}}", content);

    String response = vertexAIClient.generate(prompt);

    ObjectNode result = (ObjectNode) mapper.readTree(JsonResponseExtractor.object(response));
    normalizeTextField(result, "businessCapability");
    normalizeTextField(result, "technicalCapability");

    return mapper.treeToValue(result, ClassificationResult.class);
  }

  private void normalizeTextField(ObjectNode result, String fieldName) {
    if (result.get(fieldName) instanceof ArrayNode values) {
      result.put(fieldName, String.join(", ", mapper.convertValue(values, List.class)));
    }
  }
}
