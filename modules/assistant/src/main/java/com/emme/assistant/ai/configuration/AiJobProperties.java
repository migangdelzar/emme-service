package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai.jobs")
public record AiJobProperties(int workerCount, int queueCapacity, int maxAttempts, int pollLimit) {
  public AiJobProperties(int workerCount, int queueCapacity, int maxAttempts) {
    this(workerCount, queueCapacity, maxAttempts, 32);
  }

  public AiJobProperties {
    if (workerCount <= 0 || queueCapacity <= 0 || maxAttempts <= 0 || pollLimit <= 0)
      throw new IllegalArgumentException("AI job limits must be positive");
  }

  public AiJobProperties() {
    this(2, 32, 3, 32);
  }
}
