package com.emme.ai.platform.configuration;

import com.emme.ai.contracts.semantic.EmbeddingModelDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Provider infrastructure configuration bound to {@code app.ai.*}. */
@ConfigurationProperties("app.ai")
public record AiProviderProperties(
    String provider, ProviderConfig chat, EmbeddingConfig embedding, boolean mockMode) {

  public static final String DEFAULT_EMBEDDING_MODEL_NAME = EmbeddingModelDefaults.MODEL_NAME;
  public static final String DEFAULT_EMBEDDING_MODEL_VERSION = EmbeddingModelDefaults.MODEL_VERSION;
  public static final int DEFAULT_EMBEDDING_DIMENSION = EmbeddingModelDefaults.DIMENSION;

  public record ProviderConfig(String model, String baseUrl, String apiKey) {}

  public record EmbeddingConfig(String model, String baseUrl, String apiKey, Integer dimension) {
    public EmbeddingConfig {
      if (dimension == null || dimension <= 0) dimension = DEFAULT_EMBEDDING_DIMENSION;
    }
  }

  public AiProviderProperties {
    if (provider == null || provider.isBlank()) provider = "mock";
    if (chat == null) chat = new ProviderConfig("gemma4:e4b-mlx", "http://localhost:11434", null);
    if (embedding == null)
      embedding =
          new EmbeddingConfig(
              DEFAULT_EMBEDDING_MODEL_NAME,
              "http://localhost:11434",
              null,
              DEFAULT_EMBEDDING_DIMENSION);
  }

  public int embeddingDimension() {
    return embedding.dimension();
  }

  public String embeddingModelVersion() {
    return DEFAULT_EMBEDDING_MODEL_VERSION;
  }
}
