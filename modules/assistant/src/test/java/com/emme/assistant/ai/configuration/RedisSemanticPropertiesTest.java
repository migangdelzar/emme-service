package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedisSemanticPropertiesTest {

  @Test
  void providesSafeDefaultsForTheOptionalRedisSemanticProjection() {
    RedisSemanticProperties properties =
        new RedisSemanticProperties(true, null, null, null, null, null, null, false, null);

    assertThat(properties.host()).isEqualTo("localhost");
    assertThat(properties.port()).isEqualTo(6379);
    assertThat(properties.indexName()).isEqualTo("emme-ai-semantic-cache");
    assertThat(properties.prefix()).isEqualTo("emme:ai:semantic-cache:");
    assertThat(properties.embeddingModelVersion()).isEqualTo("ollama-embeddinggemma:300m");
    assertThat(properties.embeddingDimension()).isEqualTo(768);
    assertThat(properties.toolSearchMaxResults()).isEqualTo(5);
  }

  @Test
  void rejectsAnInvalidRedisPortOrEmbeddingDimension() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new RedisSemanticProperties(
                    true, "localhost", 0, "index", "prefix", "model", 768, false, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("port must be between 1 and 65535");

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new RedisSemanticProperties(
                    true, "localhost", 6379, "index", "prefix", "model", 0, false, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("embeddingDimension must be positive");
  }
}
