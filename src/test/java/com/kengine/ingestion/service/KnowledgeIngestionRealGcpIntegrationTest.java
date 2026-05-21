package com.kengine.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.kengine.ingestion.dto.ClassificationResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    properties = {"gcp.project-id=test-project", "gcp.pubsub.subscription-id=test-subscription"})
@EnabledIfEnvironmentVariable(named = "RUN_REAL_GCP_INTEGRATION_TESTS", matches = "true")
class KnowledgeIngestionRealGcpIntegrationTest {

  @Autowired private KnowledgeIngestionService ingestionService;

  @Autowired private Storage storage;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void ingestDummyDocumentWithRealGcpCalls() throws Exception {
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

      Map<String, Object> row =
          jdbcTemplate.queryForMap(
              """
              SELECT chunk_id, artifact_id, project_id, domain, subdomain, embedding::text AS embedding
              FROM knowledge.semantic_chunks
              WHERE content LIKE ?
              ORDER BY created_at DESC
              LIMIT 1
              """,
              "%" + marker + "%");

      assertFalse(row.get("chunk_id").toString().isBlank());
      assertFalse(row.get("artifact_id").toString().isBlank());
      assertTrue(projectId.equals(row.get("project_id")));
      assertFalse(row.get("domain").toString().isBlank());
      assertNotNull(row.get("embedding"));
      assertFalse(row.get("embedding").toString().isBlank(), "Expected persisted embedding values");
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
