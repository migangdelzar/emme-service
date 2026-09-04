package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.tenant.AiTenantContextResolver;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.adapter.in.messaging.SemanticCacheInvalidationListener;
import com.emme.assistant.ai.adapter.out.persistence.JacksonSemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.SemanticCacheHotStore;
import com.emme.assistant.ai.application.port.out.SemanticCachePayloadCodec;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.semantic.SemanticCacheIdentity;
import com.emme.assistant.ai.application.semantic.SemanticCacheInvalidationService;
import com.emme.assistant.ai.application.semantic.SemanticCachePolicy;
import com.emme.assistant.ai.application.semantic.SemanticCacheResolver;
import com.emme.assistant.ai.application.semantic.SemanticChatCache;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
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
    return new SemanticCachePolicy(properties.minimumSimilarity(), properties.minimumMargin());
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticCacheResolver semanticCacheResolver(
      SemanticCachePort cache,
      SemanticCachePolicy semanticCachePolicy,
      SemanticMetrics metrics,
      AiTraceRecorder traceRecorder) {
    return new SemanticCacheResolver(cache, semanticCachePolicy, metrics, traceRecorder);
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticCacheInvalidationService semanticCacheInvalidationService(
      SemanticCachePort cache,
      Optional<SemanticCacheHotStore> hotStore,
      SemanticMetrics metrics,
      AiTraceRecorder traceRecorder,
      Optional<AiTenantContextResolver> tenantContextResolver) {
    return new SemanticCacheInvalidationService(
        cache, hotStore, metrics, traceRecorder, tenantContextResolver);
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticCacheInvalidationListener semanticCacheInvalidationListener(
      SemanticCacheInvalidationService invalidation) {
    return new SemanticCacheInvalidationListener(invalidation);
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
      Optional<SemanticCacheHotStore> hotStore,
      SemanticMetrics metrics,
      AiProviderProperties aiProperties,
      AiTraceRecorder traceRecorder) {
    return new SemanticChatCache(
        embeddings,
        resolver,
        cache,
        codec,
        clock,
        properties.promptVersion(),
        properties.ttl(),
        hotStore,
        metrics,
        aiProperties.embeddingModelConfiguration(),
        new SemanticCacheIdentity(
            aiProperties.provider(),
            aiProperties.chat().model(),
            properties.knowledgeVersion(),
            properties.policyVersion(),
            properties.sourceVersion(),
            "INTERNAL",
            properties.locale(),
            properties.quoteTemplateVersion()),
        properties.locale(),
        properties.quoteTemplateVersion(),
        traceRecorder);
  }
}
