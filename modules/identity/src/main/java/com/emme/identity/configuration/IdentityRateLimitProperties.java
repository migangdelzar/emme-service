package com.emme.identity.configuration;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Typed login rate-limit settings owned by Identity. */
@ConfigurationProperties(prefix = "app.identity.login-rate-limit")
public record IdentityRateLimitProperties(
    int maxAttempts, long windowMs, List<String> trustedProxies) {

  public IdentityRateLimitProperties(
      @DefaultValue("5") int maxAttempts,
      @DefaultValue("60000") long windowMs,
      @DefaultValue List<String> trustedProxies) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be greater than zero");
    }
    if (windowMs < 1) {
      throw new IllegalArgumentException("windowMs must be greater than zero");
    }
    this.maxAttempts = maxAttempts;
    this.windowMs = windowMs;
    this.trustedProxies = List.copyOf(Objects.requireNonNull(trustedProxies, "trustedProxies"));
  }

  public static IdentityRateLimitProperties defaults() {
    return new IdentityRateLimitProperties(5, 60_000L, List.of());
  }
}
