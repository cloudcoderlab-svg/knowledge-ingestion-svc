package com.kengine.knowledge.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

  @Bean(name = "ingestionProcessExecutor")
  public Executor ingestionProcessExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("ingestion-process-");
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(20);
    executor.initialize();
    return executor;
  }

  @Bean(name = "ingestionDocumentExecutor")
  public Executor ingestionDocumentExecutor(
      @Value("${knowledge-engine.ingestion.parallelism:2}") int parallelism) {
    int workers = Math.max(1, parallelism);
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("ingestion-document-");
    executor.setCorePoolSize(workers);
    executor.setMaxPoolSize(workers);
    executor.setQueueCapacity(200);
    executor.initialize();
    return executor;
  }
}
