package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiPropertiesTest {

  @Test
  void usesTheMacOptimizedGemmaDefaultsWhenNoProviderConfigurationIsSupplied() {
    AiProperties properties = new AiProperties(null, null, null, false);

    assertThat(properties.chat().model()).isEqualTo("gemma4:e4b-mlx");
    assertThat(properties.embedding().model()).isEqualTo("embeddinggemma:300m");
    assertThat(properties.embeddingDimension()).isEqualTo(768);
  }

  @Test
  void usesTheEmbeddingGemmaDimensionWhenAnEmbeddingConfigurationOmitsIt() {
    AiProperties properties =
        new AiProperties(
            "ollama",
            new AiProperties.ProviderConfig("gemma4:e4b-mlx", "http://localhost:11434", null),
            new AiProperties.EmbeddingConfig(
                "embeddinggemma:300m", "http://localhost:11434", null, null),
            false);

    assertThat(properties.embeddingDimension()).isEqualTo(768);
  }
}
