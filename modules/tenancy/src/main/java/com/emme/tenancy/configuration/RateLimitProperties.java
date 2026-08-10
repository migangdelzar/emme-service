package com.emme.tenancy.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed per-tenant API rate-limiting configuration. */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(Boolean enabled, int maxRequests, Duration window) {

  public RateLimitProperties {
    if (enabled == null) {
      enabled = true;
    }
    if (maxRequests <= 0) {
      maxRequests = 100;
    }
    if (window == null) {
      window = Duration.ofMinutes(1);
    }
  }
}
