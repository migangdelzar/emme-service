package com.emme.identity.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed login rate-limit settings owned by Identity. */
@ConfigurationProperties(prefix = "app.identity.login-rate-limit")
public class IdentityRateLimitProperties {

  private int maxAttempts = 5;
  private long windowMs = 60_000L;
  private List<String> trustedProxies = List.of();

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be greater than zero");
    }
    this.maxAttempts = maxAttempts;
  }

  public long getWindowMs() {
    return windowMs;
  }

  public void setWindowMs(long windowMs) {
    if (windowMs < 1) {
      throw new IllegalArgumentException("windowMs must be greater than zero");
    }
    this.windowMs = windowMs;
  }

  public List<String> getTrustedProxies() {
    return trustedProxies;
  }

  public void setTrustedProxies(List<String> trustedProxies) {
    this.trustedProxies = List.copyOf(trustedProxies);
  }
}
