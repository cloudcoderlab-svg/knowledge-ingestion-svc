package com.kengine.ingestion.helper;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PromptLoader {

  public String load(String path) throws Exception {

    return new String(
        new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }
}
