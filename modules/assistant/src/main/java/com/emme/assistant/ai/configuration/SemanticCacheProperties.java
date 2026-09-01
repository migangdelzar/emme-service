package com.emme.assistant.ai.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the optional PostgreSQL semantic response cache. */
@ConfigurationProperties("app.ai.semantic-cache")
public record SemanticCacheProperties(
    boolean enabled,
    Double minimumSimilarity,
    Double minimumMargin,
    Duration ttl,
    String promptVersion,
    String knowledgeVersion,
    String policyVersion,
    String sourceVersion,
    String locale,
    String quoteTemplateVersion) {

  public SemanticCacheProperties(
      boolean enabled, Double minimumSimilarity, Duration ttl, String promptVersion) {
    this(enabled, minimumSimilarity, null, ttl, promptVersion, null, null, null, null, null);
  }

  public SemanticCacheProperties(
      boolean enabled,
      Double minimumSimilarity,
      Double minimumMargin,
      Duration ttl,
      String promptVersion) {
    this(enabled, minimumSimilarity, minimumMargin, ttl, promptVersion, null, null, null, null, null);
  }

  public SemanticCacheProperties {
    minimumSimilarity = minimumSimilarity == null ? 0.95 : minimumSimilarity;
    minimumMargin = minimumMargin == null ? 0.05 : minimumMargin;
    ttl = ttl == null ? Duration.ofMinutes(5) : ttl;
    promptVersion = promptVersion == null ? "chat-v1" : promptVersion;
    knowledgeVersion = knowledgeVersion == null ? "knowledge-v1" : knowledgeVersion;
    policyVersion = policyVersion == null ? "policy-v1" : policyVersion;
    sourceVersion = sourceVersion == null ? "source-v1" : sourceVersion;
    locale = locale == null ? "es-MX" : locale;
    quoteTemplateVersion = quoteTemplateVersion == null ? "quote-template-v1" : quoteTemplateVersion;
    if (!Double.isFinite(minimumSimilarity)
        || minimumSimilarity < -1.0
        || minimumSimilarity > 1.0) {
      throw new IllegalArgumentException("minimumSimilarity must be between -1 and 1");
    }
    if (!Double.isFinite(minimumMargin) || minimumMargin < 0.0 || minimumMargin > 2.0) {
      throw new IllegalArgumentException("minimumMargin must be between 0 and 2");
    }
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("ttl must be positive");
    }
    if (promptVersion.isBlank()) {
      throw new IllegalArgumentException("promptVersion must not be blank");
    }
    if (knowledgeVersion.isBlank()) {
      throw new IllegalArgumentException("knowledgeVersion must not be blank");
    }
    if (policyVersion.isBlank()) {
      throw new IllegalArgumentException("policyVersion must not be blank");
    }
    if (sourceVersion.isBlank()) {
      throw new IllegalArgumentException("sourceVersion must not be blank");
    }
    if (locale.isBlank()) {
      throw new IllegalArgumentException("locale must not be blank");
    }
    if (quoteTemplateVersion.isBlank()) {
      throw new IllegalArgumentException("quoteTemplateVersion must not be blank");
    }
  }
}
