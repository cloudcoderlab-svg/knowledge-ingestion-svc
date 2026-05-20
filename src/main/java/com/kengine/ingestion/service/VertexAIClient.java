package com.kengine.ingestion.service;

import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VertexAIClient {

  @Value("${vertex.project-id}")
  private String projectId;

  @Value("${vertex.location}")
  private String location;

  @Value("${vertex.classification-model-name:${vertex.model-name}}")
  private String classificationModelName;

  @Value("${vertex.embedding-model-name}")
  private String embeddingModelName;

  private VertexAI vertexAI;
  private GenerativeModel classificationModel;
  private PredictionServiceClient predictionServiceClient;

  @PostConstruct
  public void init() throws Exception {
    this.vertexAI = new VertexAI(projectId, location);
    this.classificationModel = new GenerativeModel(classificationModelName, vertexAI);
    this.predictionServiceClient =
        PredictionServiceClient.create(
            PredictionServiceSettings.newBuilder().setEndpoint(apiEndpoint()).build());
  }

  @PreDestroy
  public void close() {
    if (predictionServiceClient != null) {
      predictionServiceClient.close();
    }
  }

  public String generate(String prompt) {
    try {
      GenerateContentResponse response = classificationModel.generateContent(prompt);
      return response.getCandidates(0).getContent().getParts(0).getText();
    } catch (Exception e) {
      throw new RuntimeException("Failed to call Vertex AI", e);
    }
  }

  public List<Float> embedding(String text) {
    try {
      com.google.protobuf.Value instance =
          com.google.protobuf.Value.newBuilder()
              .setStructValue(
                  com.google.protobuf.Struct.newBuilder()
                      .putFields(
                          "content",
                          com.google.protobuf.Value.newBuilder().setStringValue(text).build()))
              .build();

      PredictResponse response =
          predictionServiceClient.predict(
              modelResourceName(),
              List.of(instance),
              com.google.protobuf.Value.newBuilder().build());

      return response
          .getPredictions(0)
          .getStructValue()
          .getFieldsOrThrow("embeddings")
          .getStructValue()
          .getFieldsOrThrow("values")
          .getListValue()
          .getValuesList()
          .stream()
          .map(value -> (float) value.getNumberValue())
          .toList();
    } catch (Exception e) {
      throw new RuntimeException("Failed to call Vertex AI embedding", e);
    }
  }

  private String apiEndpoint() {
    if ("global".equals(location)) {
      return "aiplatform.googleapis.com:443";
    }
    return location + "-aiplatform.googleapis.com:443";
  }

  private String modelResourceName() {
    return String.format(
        "projects/%s/locations/%s/publishers/google/models/%s",
        projectId, location, embeddingModelName);
  }
}
