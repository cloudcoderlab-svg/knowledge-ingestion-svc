package com.kengine.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GcsNotificationListener {

  private final KnowledgeIngestionService ingestionService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  @Value("${gcp.project-id}")
  private String projectId;

  @Value("${gcp.pubsub.subscription-id}")
  private String subscriptionId;

  private Subscriber subscriber;

  @PostConstruct
  public void startListening() {
    if (projectId == null || subscriptionId == null || "test-project".equals(projectId)) {
      log.warn("Skipping Pub/Sub subscriber initialization in test or missing config");
      return;
    }
    ProjectSubscriptionName subscriptionName =
        ProjectSubscriptionName.of(projectId, subscriptionId);

    subscriber = Subscriber.newBuilder(subscriptionName, getMessageReceiver()).build();
    subscriber.startAsync().awaitRunning();
    log.info("Started Pub/Sub subscriber for {}", subscriptionName);
  }

  public MessageReceiver getMessageReceiver() {
    return (PubsubMessage message, AckReplyConsumer consumer) -> {
      executorService.submit(
          () -> {
            try {
              String data = message.getData().toStringUtf8();
              log.info("Received Pub/Sub message: {}", data);

              JsonNode node = objectMapper.readTree(data);
              String bucket = node.get("bucket").asText();
              String name = node.get("name").asText();

              ingestionService.ingestFromGcs(bucket, name);
              consumer.ack();
            } catch (Exception e) {
              log.error("Error processing GCS notification", e);
              consumer.nack();
            }
          });
    };
  }

  @PreDestroy
  public void stopListening() {
    if (subscriber != null) {
      subscriber.stopAsync().awaitTerminated();
    }
    executorService.shutdown();
  }
}
