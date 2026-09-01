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
}
