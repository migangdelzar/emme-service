package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedisSemanticPropertiesTest {

  @Test
  void providesSafeDefaultsForTheOptionalRedisSemanticProjection() {
    RedisSemanticProperties properties =
        new RedisSemanticProperties(true, null, null, null, null, null, false, null);

    assertThat(properties.host()).isEqualTo("localhost");
    assertThat(properties.port()).isEqualTo(6379);
    assertThat(properties.indexName()).isEqualTo("emme-ai-semantic-cache");
    assertThat(properties.prefix()).isEqualTo("emme:ai:semantic-cache:");
    assertThat(properties.embeddingModelVersion()).isEqualTo("ollama-embeddinggemma:300m");
    assertThat(properties.toolSearchMaxResults()).isEqualTo(5);
  }

  @Test
  void rejectsAnInvalidRedisPort() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new RedisSemanticProperties(
                    true, "localhost", 0, "index", "prefix", "model", false, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("port must be between 1 and 65535");
  }
}
