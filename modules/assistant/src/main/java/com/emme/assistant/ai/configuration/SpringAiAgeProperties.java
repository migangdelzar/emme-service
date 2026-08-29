package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Feature-gated settings for the optional Apache AGE derived graph projection. */
@ConfigurationProperties("app.ai.age")
public record SpringAiAgeProperties(boolean enabled, String graphPrefix, int retrievalLimit) {

  public SpringAiAgeProperties {
    graphPrefix = graphPrefix == null || graphPrefix.isBlank() ? "emme_ai_graph_" : graphPrefix;
    if (!graphPrefix.matches("[a-z][a-z0-9_]{0,39}")) {
      throw new IllegalArgumentException(
          "graphPrefix must contain only lowercase letters, digits, and underscores");
    }
    if (retrievalLimit == 0) {
      retrievalLimit = 5;
    }
    if (retrievalLimit < 1 || retrievalLimit > 50) {
      throw new IllegalArgumentException("retrievalLimit must be between 1 and 50");
    }
  }
}
