package com.kengine.ingestion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ResourceConfig {
  private String key;
  private String value;
  private String unit;
}
