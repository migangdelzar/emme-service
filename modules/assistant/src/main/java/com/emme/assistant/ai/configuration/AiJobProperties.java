package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.ai.jobs")
public record AiJobProperties(
    @DefaultValue("2") int workerCount,
    @DefaultValue("32") int queueCapacity,
    @DefaultValue("3") int maxAttempts,
    @DefaultValue("32") int pollLimit) {
  public AiJobProperties(int workerCount, int queueCapacity, int maxAttempts) {
    this(workerCount, queueCapacity, maxAttempts, 32);
  }

  @ConstructorBinding
  public AiJobProperties {
    if (workerCount <= 0 || queueCapacity <= 0 || maxAttempts <= 0 || pollLimit <= 0)
      throw new IllegalArgumentException("AI job limits must be positive");
  }

  public AiJobProperties() {
    this(2, 32, 3, 32);
  }
}
