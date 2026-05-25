package com.kengine.ingestion.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    // Register Java 8 Date/Time module for OffsetDateTime, LocalDateTime, etc.
    mapper.registerModule(new JavaTimeModule());

    // Configure to handle unknown properties gracefully
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Handle empty strings as null objects
    mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

    // Allow coercion of scalars to and from empty strings
    mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

    // Accept single value as array
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    // Don't fail on reading null values for primitive types
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

    // Don't fail on invalid type conversions - coerce to null instead
    mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

    // Disable writing dates as timestamps (use ISO-8601 format instead)
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Pretty print for readability (can be disabled in production if needed)
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    return mapper;
  }
}
