package com.kengine.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.kengine.ingestion.dto.ClassificationResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {"gcp.project-id=test-project", "gcp.pubsub.subscription-id=test-subscription"})
class KnowledgeIngestionRealGcpIntegrationTest {

  @Autowired private KnowledgeIngestionService ingestionService;

  @Autowired private Storage storage;

  @Autowired private DatabaseClient databaseClient;

  @Test
  void ingestDummyDocumentWithRealGcpCalls() throws Exception {
    Assumptions.assumeTrue(
        Boolean.parseBoolean(System.getenv("RUN_REAL_GCP_INTEGRATION_TESTS")),
        "Set RUN_REAL_GCP_INTEGRATION_TESTS=true to run real GCP integration tests");

    String bucket =
        firstNonBlank(
            System.getProperty("integration.gcs.bucket"), System.getenv("INTEGRATION_GCS_BUCKET"));
    boolean createdBucket = false;
    if (bucket == null || bucket.isBlank()) {
      bucket = "knowledge-ingestion-it-" + UUID.randomUUID();
      storage.create(BucketInfo.newBuilder(bucket).setLocation("US").build());
      createdBucket = true;
    }

    String marker = "real-gcp-ingestion-test-" + UUID.randomUUID();
    String projectId = "integration-tests";
    String objectName = projectId + "/" + marker + ".txt";
    String content =
        """
        Customer Master Data Management dummy document.

        Marker: %s

        The customer domain uses Golden Record management, identity resolution, and cloud
        integration workflows to synchronize Salesforce customer profiles into the enterprise
        MDM platform. The MDM platform publishes mastered customer records to downstream CRM
        and analytics consumers.

        Low level design components:
        - Customer Ingestion API service receives profile synchronization requests.
        - Customer Ingestion Worker processes incoming customer payloads.
        - Customer Ingestion Worker calls the MDM Adapter module to publish mastered records.
        - MDM Adapter writes integration events for CRM and analytics consumers.

        Created at: %s
        """
            .formatted(marker, Instant.now());

    try {
      storage.create(
          BlobInfo.newBuilder(BlobId.of(bucket, objectName)).setContentType("text/plain").build(),
          content.getBytes(StandardCharsets.UTF_8));

      ClassificationResult result = ingestionService.ingestFromGcs(bucket, objectName);

      assertNotNull(result);
      assertNotNull(result.getDomain());
      assertFalse(result.getDomain().isBlank());

      Statement statement =
          Statement.of(
              "SELECT chunk_id, artifact_id, project_id, domain, subdomain, embedding "
                  + "FROM knowledge.semantic_chunks "
                  + "WHERE content LIKE '%"
                  + marker
                  + "%' "
                  + "ORDER BY created_at DESC "
                  + "LIMIT 1");

      try (ResultSet rows = databaseClient.singleUse().executeQuery(statement)) {
        assertTrue(rows.next(), "Expected semantic_chunks row for marker " + marker);
        assertFalse(rows.getString("chunk_id").isBlank());
        assertFalse(rows.getString("artifact_id").isBlank());
        assertTrue(projectId.equals(rows.getString("project_id")));
        assertFalse(rows.getString("domain").isBlank());
        List<Double> embedding = rows.getDoubleList("embedding");
        assertNotNull(embedding);
        assertFalse(embedding.isEmpty(), "Expected persisted embedding values");
      }
    } finally {
      storage.delete(BlobId.of(bucket, objectName));
      if (createdBucket) {
        storage.delete(bucket);
      }
    }
  }

  private static String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second;
  }
}
