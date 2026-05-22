package com.kengine.ingestion.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for shared ObjectMapper bean. */
@Configuration
public class ObjectMapperConfig {

  /**
   * Creates a configured ObjectMapper bean for JSON serialization/deserialization.
   *
   * @return configured ObjectMapper instance
   */
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Configure to handle unknown properties gracefully
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Pretty print for readability (can be disabled in production if needed)
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    return mapper;
  }
}
