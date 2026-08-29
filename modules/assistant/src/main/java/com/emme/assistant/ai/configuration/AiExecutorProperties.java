package com.emme.assistant.ai.configuration;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Tunable limits for the executor pools used by AI infrastructure. */
@ConfigurationProperties("app.ai.execution")
public record AiExecutorProperties(
    @DefaultValue("4") int backgroundParallelism,
    @DefaultValue("2") int cpuParallelism,
    @DefaultValue("1") int schedulerPoolSize,
    @DefaultValue("5s") Duration modelAdmissionTimeout) {

  public AiExecutorProperties(
      int backgroundParallelism, int cpuParallelism, int schedulerPoolSize) {
    this(backgroundParallelism, cpuParallelism, schedulerPoolSize, Duration.ofSeconds(5));
  }

  @ConstructorBinding
  public AiExecutorProperties {
    requirePositive(backgroundParallelism, "backgroundParallelism");
    requirePositive(cpuParallelism, "cpuParallelism");
    requirePositive(schedulerPoolSize, "schedulerPoolSize");
    Objects.requireNonNull(modelAdmissionTimeout, "modelAdmissionTimeout must not be null");
    if (modelAdmissionTimeout.isZero() || modelAdmissionTimeout.isNegative()) {
      throw new IllegalArgumentException("modelAdmissionTimeout must be positive");
    }
  }

  private static void requirePositive(int value, String propertyName) {
    if (value <= 0) {
      throw new IllegalArgumentException(propertyName + " must be greater than zero");
    }
  }
}
