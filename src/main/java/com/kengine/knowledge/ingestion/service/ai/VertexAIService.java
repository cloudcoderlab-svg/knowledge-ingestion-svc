package com.kengine.knowledge.ingestion.service.ai;

import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VertexAIService {

  @Value("${vertex.project-id}")
  private String projectId;

  @Value("${vertex.location}")
  private String location;

  @Value("${vertex.classification-model-name:${vertex.model-name}}")
  private String classificationModelName;

  @Value("${vertex.embedding-model-name}")
  private String embeddingModelName;

  @Value("${vertex.retry.max-attempts:4}")
  private int maxAttempts;

  @Value("${vertex.retry.initial-backoff-ms:2000}")
  private long initialBackoffMs;

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
    return callWithRetry(
        "Vertex AI",
        () -> {
          GenerateContentResponse response = classificationModel.generateContent(prompt);
          return response.getCandidates(0).getContent().getParts(0).getText();
        });
  }

  /**
   * Generates content using both text and image/document input. This is used for multimodal
   * document analysis (PDFs, images with diagrams, etc.)
   *
   * @param prompt the text prompt
   * @param imageData the document/image binary data
   * @param mimeType the MIME type of the data (e.g., "application/pdf", "image/png")
   * @return the generated response text
   */
  public String generateWithImage(String prompt, byte[] imageData, String mimeType) {
    return callWithRetry(
        "Vertex AI multimodal",
        () -> {
          Content content =
              ContentMaker.fromMultiModalData(
                  prompt, PartMaker.fromMimeTypeAndData(mimeType, imageData));

          GenerateContentResponse response = classificationModel.generateContent(content);
          return ResponseHandler.getText(response);
        });
  }

  public List<Float> embedding(String text) {
    return callWithRetry(
        "Vertex AI embedding",
        () -> {
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
        });
  }

  private <T> T callWithRetry(String operation, ThrowingSupplier<T> supplier) {
    int attempts = Math.max(1, maxAttempts);
    long backoff = Math.max(0, initialBackoffMs);
    Exception last = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        return supplier.get();
      } catch (Exception e) {
        last = e;
        if (attempt == attempts || !isRetryable(e)) {
          throw new RuntimeException("Failed to call " + operation, e);
        }
        sleep(backoff * attempt);
      }
    }
    throw new RuntimeException("Failed to call " + operation, last);
  }

  private boolean isRetryable(Exception exception) {
    String message = exception.toString();
    Throwable cause = exception.getCause();
    while (cause != null) {
      message += " " + cause;
      cause = cause.getCause();
    }
    return message.contains("UNAVAILABLE")
        || message.contains("DEADLINE_EXCEEDED")
        || message.contains("429");
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting to retry Vertex AI call", e);
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

  @FunctionalInterface
  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }
}
