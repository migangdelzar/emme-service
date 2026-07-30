package com.emme.tenancy.web;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate limiting configuration properties.
 *
 * <p>Per-tenant IP-based rate limiting using Redis atomic counters. Configure via {@code
 * app.rate-limit.*} in application.yml.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(Boolean enabled, int maxRequests, Duration window) {

  /**
   * Compact constructor enforces sensible defaults when properties are omitted from configuration.
   *
   * <ul>
   *   <li>{@code enabled} defaults to {@code true}
   *   <li>{@code max-requests} defaults to 100
   *   <li>{@code window} defaults to 60 seconds
   * </ul>
   */
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
