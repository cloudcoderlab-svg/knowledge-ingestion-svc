package com.kengine.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class VertexAIClientTest {

  @Mock private PredictionServiceClient predictionServiceClient;
  @Mock private GenerativeModel classificationModel;

  @InjectMocks private VertexAIService vertexAIService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    ReflectionTestUtils.setField(
            vertexAIService, "predictionServiceClient", predictionServiceClient);
    ReflectionTestUtils.setField(vertexAIService, "classificationModel", classificationModel);
    ReflectionTestUtils.setField(vertexAIService, "projectId", "test-project");
    ReflectionTestUtils.setField(vertexAIService, "location", "us");
    ReflectionTestUtils.setField(vertexAIService, "embeddingModelName", "gemini-embedding-2");
  }

  @Test
  void testGenerate() throws Exception {
    String prompt = "Classify this text";
    GenerateContentResponse response =
        GenerateContentResponse.newBuilder()
            .addCandidates(
                Candidate.newBuilder()
                    .setContent(
                        Content.newBuilder()
                            .addParts(Part.newBuilder().setText("classification").build())
                            .build())
                    .build())
            .build();

    when(classificationModel.generateContent(prompt)).thenReturn(response);

    String actual = vertexAIService.generate(prompt);

    assertEquals("classification", actual);
    verify(classificationModel).generateContent(prompt);
  }

  @Test
  void testEmbedding() {
    String text = "test text";
    List<Float> expectedEmbeddings = List.of(0.1f, 0.2f, 0.3f);
    Value values =
        Value.newBuilder()
            .setListValue(
                ListValue.newBuilder()
                    .addValues(Value.newBuilder().setNumberValue(0.1).build())
                    .addValues(Value.newBuilder().setNumberValue(0.2).build())
                    .addValues(Value.newBuilder().setNumberValue(0.3).build()))
            .build();
    PredictResponse response =
        PredictResponse.newBuilder()
            .addPredictions(
                Value.newBuilder()
                    .setStructValue(
                        Struct.newBuilder()
                            .putFields(
                                "embeddings",
                                Value.newBuilder()
                                    .setStructValue(
                                        Struct.newBuilder().putFields("values", values).build())
                                    .build()))
                    .build())
            .build();

    when(predictionServiceClient.predict(
            "projects/test-project/locations/us/publishers/google/models/gemini-embedding-2",
            List.of(
                Value.newBuilder()
                    .setStructValue(
                        Struct.newBuilder()
                            .putFields("content", Value.newBuilder().setStringValue(text).build()))
                    .build()),
            Value.newBuilder().build()))
        .thenReturn(response);

    List<Float> actualEmbeddings = vertexAIService.embedding(text);

    assertEquals(expectedEmbeddings, actualEmbeddings);
    verify(predictionServiceClient)
        .predict(
            "projects/test-project/locations/us/publishers/google/models/gemini-embedding-2",
            List.of(
                Value.newBuilder()
                    .setStructValue(
                        Struct.newBuilder()
                            .putFields("content", Value.newBuilder().setStringValue(text).build()))
                    .build()),
            Value.newBuilder().build());
  }
}
