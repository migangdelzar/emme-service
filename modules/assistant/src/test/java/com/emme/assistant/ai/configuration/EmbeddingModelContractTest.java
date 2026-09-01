package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.platform.configuration.AiProviderProperties;
import org.junit.jupiter.api.Test;

class EmbeddingModelContractTest {

  @Test
  void usesOneEmbeddingModelVersionAcrossConfigurationProvidersAndIndexes() {
    AiProperties assistantProperties = new AiProperties(null, null, null, false);
    AiProviderProperties platformProperties = new AiProviderProperties(null, null, null, false);
    RedisSemanticProperties redisProperties =
        new RedisSemanticProperties(true, null, null, null, null, null, null, false, null);

    assertThat(assistantProperties.embeddingModelVersion())
        .isEqualTo(AiProviderProperties.DEFAULT_EMBEDDING_MODEL_VERSION)
        .isEqualTo(redisProperties.embeddingModelVersion());
    assertThat(platformProperties.embeddingModelVersion())
        .isEqualTo(assistantProperties.embeddingModelVersion());
    assertThat(assistantProperties.embedding().model())
        .isEqualTo(AiProviderProperties.DEFAULT_EMBEDDING_MODEL_NAME);
    assertThat(assistantProperties.embeddingDimension())
        .isEqualTo(AiProviderProperties.DEFAULT_EMBEDDING_DIMENSION)
        .isEqualTo(redisProperties.embeddingDimension());
  }
}
