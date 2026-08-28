package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Feature-gated thresholds for vector intent and tool routing. */
@ConfigurationProperties("app.ai.semantic-routing")
public record SemanticRoutingProperties(
    boolean enabled, String locale, Double minimumTop1Similarity, Double minimumMargin) {

  public SemanticRoutingProperties {
    locale = locale == null ? "es-MX" : locale;
    minimumTop1Similarity = minimumTop1Similarity == null ? 0.90 : minimumTop1Similarity;
    minimumMargin = minimumMargin == null ? 0.10 : minimumMargin;
    requireText(locale, "locale");
    if (!Double.isFinite(minimumTop1Similarity)
        || minimumTop1Similarity < -1.0
        || minimumTop1Similarity > 1.0) {
      throw new IllegalArgumentException("minimumTop1Similarity must be between -1 and 1");
    }
    if (!Double.isFinite(minimumMargin) || minimumMargin < 0.0 || minimumMargin > 2.0) {
      throw new IllegalArgumentException("minimumMargin must be between 0 and 2");
    }
  }

  private static void requireText(String value, String field) {
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
