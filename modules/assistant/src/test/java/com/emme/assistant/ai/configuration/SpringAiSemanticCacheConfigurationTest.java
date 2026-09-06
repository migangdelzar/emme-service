package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.adapter.out.event.NoopSemanticCacheDependencyPublisher;
import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.semantic.SemanticCachePolicy;
import com.emme.assistant.ai.application.semantic.SemanticCacheResolver;
import com.emme.assistant.ai.application.semantic.SemanticChatCache;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SpringAiSemanticCacheConfigurationTest {

  @Test
  void suppliesNoopDependencyPublisherWhenSemanticCacheIsDisabled() {
    new ApplicationContextRunner()
        .withUserConfiguration(SemanticCacheDependencyPublisherConfiguration.class)
        .withPropertyValues("app.ai.semantic-cache.enabled=false")
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(
                        com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher.class)
                    .getBean(com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher.class)
                    .isInstanceOf(NoopSemanticCacheDependencyPublisher.class));
  }

  @Test
  void wiresTheProviderNeutralSemanticCacheBoundary() {
    SpringAiSemanticCacheConfiguration configuration = new SpringAiSemanticCacheConfiguration();
    SemanticCacheProperties properties =
        new SemanticCacheProperties(true, 0.95, Duration.ofMinutes(5), "chat-v1");
    SemanticCacheResolver resolver =
        configuration.semanticCacheResolver(
            mock(SemanticCachePort.class),
            new SemanticCachePolicy(0.95),
            mock(SemanticMetrics.class),
            mock(AiTraceRecorder.class));

    SemanticResponseCache cache =
        configuration.semanticChatCache(
            resolver,
            mock(SemanticCachePort.class),
            mock(SemanticCachePayloadCodec.class),
            Clock.systemUTC(),
            properties,
            Optional.empty(),
            mock(SemanticMetrics.class),
            new AiProviderProperties(null, null, null, false),
            mock(AiTraceRecorder.class));

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

  @Test
  void exposesOneCanonicalSemanticCacheConstructionPath() {
    assertThat(SemanticChatCache.class.getConstructors()).hasSize(1);
  }
}
