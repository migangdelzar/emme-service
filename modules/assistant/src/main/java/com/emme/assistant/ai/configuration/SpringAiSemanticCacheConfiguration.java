package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.persistence.JacksonSemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.semantic.SemanticCachePolicy;
import com.emme.assistant.ai.application.semantic.SemanticCacheResolver;
import com.emme.assistant.ai.application.semantic.SemanticChatCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Opt-in composition root for safe semantic response caching. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SemanticCacheProperties.class)
@ConditionalOnProperty(prefix = "app.ai.semantic-cache", name = "enabled", havingValue = "true")
@ConditionalOnBean({EmbeddingModelPort.class, SemanticCachePort.class})
public class SpringAiSemanticCacheConfiguration {

  @Bean
  @ConditionalOnMissingBean
  SemanticCachePolicy semanticCachePolicy(SemanticCacheProperties properties) {
    return new SemanticCachePolicy(properties.minimumSimilarity());
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticCacheResolver semanticCacheResolver(
      SemanticCachePort cache, SemanticCachePolicy semanticCachePolicy) {
    return new SemanticCacheResolver(cache, semanticCachePolicy);
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticCachePayloadCodec semanticCachePayloadCodec(ObjectMapper objectMapper) {
    return new JacksonSemanticCachePayloadCodec(objectMapper);
  }

  @Bean(name = "aiCacheClock")
  @ConditionalOnMissingBean
  Clock aiCacheClock() {
    return Clock.systemUTC();
  }

  @Bean
  @ConditionalOnMissingBean(SemanticResponseCache.class)
  SemanticResponseCache semanticChatCache(
      EmbeddingModelPort embeddings,
      SemanticCacheResolver resolver,
      SemanticCachePort cache,
      SemanticCachePayloadCodec codec,
      @Qualifier("aiCacheClock") Clock clock,
      SemanticCacheProperties properties,
      Optional<SemanticCacheHotStore> hotStore) {
    return new SemanticChatCache(
        embeddings,
        resolver,
        cache,
        codec,
        clock,
        properties.promptVersion(),
        properties.ttl(),
        hotStore);
  }
}
