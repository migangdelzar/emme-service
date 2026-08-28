package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the opt-in LangGraph4j workflow boundary. */
@ConfigurationProperties("app.ai.langgraph")
public record LangGraphProperties(boolean enabled, String graphVersion) {

  public LangGraphProperties {
    if (graphVersion == null || graphVersion.isBlank()) {
      graphVersion = "quote-v1";
    }
  }
}
