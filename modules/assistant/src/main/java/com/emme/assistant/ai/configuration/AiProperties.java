package com.emme.assistant.ai.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI configuration — binds to app.ai.* in application.yml.
 *
 * <p>Example: app: ai: provider: ollama chat: model: gemma3:4b base-url: http://localhost:11434
 * embedding: model: bge-m3 base-url: http://localhost:11434 dimension: 1024
 */
@ConfigurationProperties("app.ai")
public record AiProperties(
    String provider, ProviderConfig chat, EmbeddingConfig embedding, boolean mockMode) {
  public record ProviderConfig(String model, String baseUrl, String apiKey) {}

  public record EmbeddingConfig(String model, String baseUrl, String apiKey, Integer dimension) {
    public EmbeddingConfig {
      if (dimension == null || dimension <= 0) dimension = 1024;
    }
  }

  public AiProperties {
    if (provider == null || provider.isBlank()) provider = "mock";
    if (chat == null) chat = new ProviderConfig("gemma3:4b", "http://localhost:11434", null);
    if (embedding == null)
      embedding = new EmbeddingConfig("bge-m3", "http://localhost:11434", null, 1024);
  }

  /** Single source of truth for the vector dimension used across schema, mock, and doctor. */
  public int embeddingDimension() {
    return embedding.dimension();
  }
}
