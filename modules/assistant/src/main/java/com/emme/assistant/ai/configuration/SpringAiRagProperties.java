package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Explicit settings for the optional Spring AI retrieval-augmented answer path. */
@ConfigurationProperties("app.ai.spring-rag")
public record SpringAiRagProperties(boolean enabled, int retrievalLimit) {

  public SpringAiRagProperties {
    if (retrievalLimit == 0) {
      retrievalLimit = 5;
    }
    if (retrievalLimit < 1 || retrievalLimit > 20) {
      throw new IllegalArgumentException("retrievalLimit must be between 1 and 20");
    }
  }
}
