package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Opt-in version metadata for the Spring AI structured extraction client. */
@ConfigurationProperties("app.ai.spring-extraction")
public record SpringAiExtractionProperties(
  boolean enabled, String modelVersion, String promptVersion, String schemaVersion) {

  public SpringAiExtractionProperties {
    modelVersion = modelVersion == null ? "ollama-gemma3-vision" : modelVersion;
    promptVersion = promptVersion == null ? "nail-design-v1" : promptVersion;
    schemaVersion = schemaVersion == null ? "nail-features-v1" : schemaVersion;
    modelVersion = requireText(modelVersion, "modelVersion");
    promptVersion = requireText(promptVersion, "promptVersion");
    schemaVersion = requireText(schemaVersion, "schemaVersion");
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
