package com.emme.assistant.ai.configuration;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Explicit ordered registry of named Spring AI chat clients. */
@ConfigurationProperties("app.ai.spring-chat")
public record SpringAiChatProperties(boolean enabled, List<Provider> providers) {

  public SpringAiChatProperties {
    providers = providers == null ? List.of() : List.copyOf(providers);
  }

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
