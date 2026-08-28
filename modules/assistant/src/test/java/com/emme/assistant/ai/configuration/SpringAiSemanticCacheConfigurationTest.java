package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.semantic.SemanticCachePolicy;
import com.emme.assistant.ai.application.semantic.SemanticCacheResolver;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SpringAiSemanticCacheConfigurationTest {

  @Test
  void wiresTheProviderNeutralSemanticCacheBoundary() {
    SpringAiSemanticCacheConfiguration configuration = new SpringAiSemanticCacheConfiguration();
    SemanticCacheProperties properties =
        new SemanticCacheProperties(true, 0.95, Duration.ofMinutes(5), "chat-v1");
    SemanticCacheResolver resolver =
        configuration.semanticCacheResolver(
            mock(SemanticCachePort.class), new SemanticCachePolicy(0.95));

    SemanticResponseCache cache =
        configuration.semanticChatCache(
            mock(EmbeddingModelPort.class),
            resolver,
            mock(SemanticCachePort.class),
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            properties);

    assertThat(cache)
        .isInstanceOf(com.emme.assistant.ai.application.semantic.SemanticChatCache.class);
  }

  @Test
  void suppliesSafeDefaults() {
    SemanticCacheProperties properties = new SemanticCacheProperties(true, null, null, null);

    assertThat(properties.minimumSimilarity()).isEqualTo(0.95);
    assertThat(properties.ttl()).isEqualTo(Duration.ofMinutes(5));
    assertThat(properties.promptVersion()).isEqualTo("chat-v1");
  }
}
