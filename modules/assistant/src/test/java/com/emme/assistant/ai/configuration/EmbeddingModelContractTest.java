package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.platform.configuration.AiProviderProperties;
import org.junit.jupiter.api.Test;

class EmbeddingModelContractTest {

  @Test
  void usesOneEmbeddingModelVersionAcrossConfigurationProvidersAndIndexes() {
    AiProviderProperties properties = new AiProviderProperties(null, null, null, false);
    RedisSemanticProperties redisProperties =
        new RedisSemanticProperties(true, null, null, null, null, null, false, null);

    assertThat(properties.embeddingModelVersion())
        .isEqualTo(AiProviderProperties.DEFAULT_EMBEDDING_MODEL_VERSION)
        .isEqualTo(redisProperties.embeddingModelVersion());
    assertThat(properties.embedding().model())
        .isEqualTo(AiProviderProperties.DEFAULT_EMBEDDING_MODEL_NAME);
    assertThat(properties.embeddingDimension())
        .isEqualTo(AiProviderProperties.DEFAULT_EMBEDDING_DIMENSION);
  }
}
