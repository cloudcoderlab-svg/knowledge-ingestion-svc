package com.kengine.ingestion.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GcsNotificationListenerTest {

  @Mock private KnowledgeIngestionService ingestionService;

  @InjectMocks private GcsNotificationListener listener;

  @Mock private AckReplyConsumer consumer;

  @Test
  void testMessageProcessing() throws Exception {
    // Arrange
    String jsonPayload = "{\"bucket\": \"test-bucket\", \"name\": \"test-file.pdf\"}";
    PubsubMessage message =
        PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(jsonPayload)).build();

    MessageReceiver receiver = listener.getMessageReceiver();

    // Act
    receiver.receiveMessage(message, consumer);

    // Allow some time for the executor service to process
    TimeUnit.MILLISECONDS.sleep(500);

    // Assert
    verify(ingestionService, times(1)).ingestFromGcs("test-bucket", "test-file.pdf");
    verify(consumer, times(1)).ack();
  }

  @Test
  void testMessageProcessingFailure() throws Exception {
    // Arrange
    String jsonPayload = "{\"bucket\": \"test-bucket\", \"name\": \"error-file.pdf\"}";
    PubsubMessage message =
        PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(jsonPayload)).build();

    doThrow(new RuntimeException("Simulated error"))
        .when(ingestionService)
        .ingestFromGcs(anyString(), anyString());

    MessageReceiver receiver = listener.getMessageReceiver();

    // Act
    receiver.receiveMessage(message, consumer);

    // Allow some time for the executor service to process
    TimeUnit.MILLISECONDS.sleep(500);

    // Assert
    verify(consumer, times(1)).nack();
  }
}
