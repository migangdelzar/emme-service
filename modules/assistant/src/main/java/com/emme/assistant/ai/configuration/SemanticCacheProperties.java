package com.emme.assistant.ai.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the optional PostgreSQL semantic response cache. */
@ConfigurationProperties("app.ai.semantic-cache")
public record SemanticCacheProperties(
    boolean enabled, Double minimumSimilarity, Duration ttl, String promptVersion) {

  public SemanticCacheProperties {
    minimumSimilarity = minimumSimilarity == null ? 0.95 : minimumSimilarity;
    ttl = ttl == null ? Duration.ofMinutes(5) : ttl;
    promptVersion = promptVersion == null ? "chat-v1" : promptVersion;
    if (!Double.isFinite(minimumSimilarity)
        || minimumSimilarity < -1.0
        || minimumSimilarity > 1.0) {
      throw new IllegalArgumentException("minimumSimilarity must be between -1 and 1");
    }
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("ttl must be positive");
    }
    if (promptVersion.isBlank()) {
      throw new IllegalArgumentException("promptVersion must not be blank");
    }
  }
}
