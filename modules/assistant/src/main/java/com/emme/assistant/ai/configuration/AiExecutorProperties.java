package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Tunable limits for the executor pools used by AI infrastructure. */
@ConfigurationProperties("app.ai.execution")
public record AiExecutorProperties(
    @DefaultValue("4") int backgroundParallelism,
    @DefaultValue("2") int cpuParallelism,
    @DefaultValue("1") int schedulerPoolSize) {

  public AiExecutorProperties {
    requirePositive(backgroundParallelism, "backgroundParallelism");
    requirePositive(cpuParallelism, "cpuParallelism");
    requirePositive(schedulerPoolSize, "schedulerPoolSize");
  }

  private static void requirePositive(int value, String propertyName) {
    if (value <= 0) {
      throw new IllegalArgumentException(propertyName + " must be greater than zero");
    }
  }
}
