package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiProviderPropertiesTest {

  @Test
  void usesTheMacOptimizedGemmaDefaultsWhenNoProviderConfigurationIsSupplied() {
    AiProviderProperties properties = new AiProviderProperties(null, null, null, false);

    assertThat(properties.chat().model()).isEqualTo("gemma4:e4b-mlx");
    assertThat(properties.embedding().model()).isEqualTo("embeddinggemma:300m");
    assertThat(properties.embeddingDimension()).isEqualTo(768);
  }

  @Test
  void defaultsAnExplicitlyConfiguredEmbeddingWithoutDimensionTo768() {
    AiProviderProperties properties =
        new AiProviderProperties(
            null,
            null,
            new AiProviderProperties.EmbeddingConfig(
                "embeddinggemma:300m", "http://localhost", null, null),
            false);

    assertThat(properties.embeddingDimension()).isEqualTo(768);
  }

  @Test
  void bindsTheConfiguredEmbeddingVersionIntoTheSharedIdentity() {
    AiProviderProperties properties =
        new AiProviderProperties(
            null,
            null,
            new AiProviderProperties.EmbeddingConfig(
                "embeddinggemma:300m", "http://localhost", null, 384, "gemma-v2"),
            false);

    assertThat(properties.embeddingModelConfiguration().modelVersion()).isEqualTo("gemma-v2");
    assertThat(properties.embeddingModelConfiguration().dimension()).isEqualTo(384);
  }

  @Test
  void derivesAStableNonDefaultModelIdentityWhenItsVersionIsOmitted() {
    AiProviderProperties properties =
        new AiProviderProperties(
            "ollama",
            null,
            new AiProviderProperties.EmbeddingConfig(
                "bge-m3", "http://localhost", null, 1024, null),
            false);

    assertThat(properties.embeddingModelConfiguration().modelName()).isEqualTo("bge-m3");
    assertThat(properties.embeddingModelConfiguration().modelVersion()).isEqualTo("bge-m3");
  }
}
