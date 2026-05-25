package com.kengine.ingestion.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.FileInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CloudStorageConfig {

  @Value("${gcp.credentials.file:#{null}}")
  private String credentialsFile;

  @Value("${gcp.project-id}")
  private String projectId;

  @Bean
  public Storage storage() {
    try {
      StorageOptions.Builder builder = StorageOptions.newBuilder().setProjectId(projectId);

      // If service account credentials file is provided, use it
      if (credentialsFile != null && !credentialsFile.isEmpty()) {
        log.info("Loading service account credentials from: {}", credentialsFile);
        try (FileInputStream credentialsStream = new FileInputStream(credentialsFile)) {
          GoogleCredentials credentials = ServiceAccountCredentials.fromStream(credentialsStream);
          builder.setCredentials(credentials);
          log.info("Successfully loaded service account credentials");
        }
      } else {
        log.info("Using Application Default Credentials");
        // Use default credentials (ADC)
        builder.setCredentials(GoogleCredentials.getApplicationDefault());
      }

      return builder.build().getService();
    } catch (IOException e) {
      log.error("Failed to load Google Cloud Storage credentials", e);
      throw new RuntimeException("Failed to initialize Google Cloud Storage", e);
    }
  }
}
