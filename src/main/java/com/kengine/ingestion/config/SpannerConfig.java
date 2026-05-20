package com.kengine.ingestion.config;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpannerConfig {
  @Value("${spanner.project-id}")
  private String projectId;

  @Value("${spanner.instance-id}")
  private String instanceId;

  @Value("${spanner.database-id}")
  private String databaseId;

  @Bean
  public DatabaseClient databaseClient() {

    Spanner spanner = SpannerOptions.newBuilder().setProjectId(projectId).build().getService();

    DatabaseId db = DatabaseId.of(projectId, instanceId, databaseId);

    return spanner.getDatabaseClient(db);
  }
}
