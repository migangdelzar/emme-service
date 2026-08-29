package com.emme.assistant.ai.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for crash recovery of durable AI mutation claims. */
@ConfigurationProperties("app.ai.tool-idempotency")
public record AiToolIdempotencyProperties(Duration claimLease) {

  public AiToolIdempotencyProperties {
    claimLease = claimLease == null ? Duration.ofMinutes(15) : claimLease;
    if (claimLease.isZero() || claimLease.isNegative()) {
      throw new IllegalArgumentException("claimLease must be positive");
    }
    if (claimLease.compareTo(Duration.ofDays(1)) > 0) {
      throw new IllegalArgumentException("claimLease must not exceed one day");
    }
  }
}
