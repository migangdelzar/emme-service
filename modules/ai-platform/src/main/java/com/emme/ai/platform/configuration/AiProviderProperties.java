package com.emme.ai.platform.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Provider infrastructure configuration bound to {@code app.ai.*}. */
@ConfigurationProperties("app.ai")
public record AiProviderProperties(
    String provider, ProviderConfig chat, EmbeddingConfig embedding, boolean mockMode) {

  public record ProviderConfig(String model, String baseUrl, String apiKey) {}

  public record EmbeddingConfig(String model, String baseUrl, String apiKey, Integer dimension) {
    public EmbeddingConfig {
      if (dimension == null || dimension <= 0) dimension = 1024;
    }
  }

  public AiProviderProperties {
    if (provider == null || provider.isBlank()) provider = "mock";
    if (chat == null) chat = new ProviderConfig("gemma4:e4b-mlx", "http://localhost:11434", null);
    if (embedding == null)
      embedding = new EmbeddingConfig("embeddinggemma:300m", "http://localhost:11434", null, 768);
  }

  public int embeddingDimension() {
    return embedding.dimension();
  }
}
