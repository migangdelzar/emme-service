package com.emme.assistant.ai.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Explicit composition settings for named Spring AI embedding model beans. */
@ConfigurationProperties("app.ai.spring-embedding")
public record SpringAiEmbeddingProperties(boolean enabled, List<Provider> providers) {

  public SpringAiEmbeddingProperties {
    providers = providers == null ? List.of() : List.copyOf(providers);
  }

  /** Maps a Spring bean name to the stable provider/model identity persisted with vectors. */
  public record Provider(String beanName, String key, String modelVersion) {
    public Provider {
      requireText(beanName, "beanName");
      requireText(key, "key");
      requireText(modelVersion, "modelVersion");
    }

    private static void requireText(String value, String field) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException(field + " must not be blank");
      }
    }
  }
}
