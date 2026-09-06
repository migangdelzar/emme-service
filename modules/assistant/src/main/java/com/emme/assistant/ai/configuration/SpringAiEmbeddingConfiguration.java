package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.ai.platform.configuration.SpringAiObservationConventions;
import com.emme.assistant.ai.application.provider.EmbeddingModelSelector;
import com.emme.assistant.ai.application.semantic.EmbeddingSemanticQueryFactory;
import com.emme.assistant.ai.application.semantic.SemanticQueryFactory;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for explicitly enabled Spring AI embedding providers. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.ai.spring-embedding", name = "enabled", havingValue = "true")
public class SpringAiEmbeddingConfiguration {

  EmbeddingModel ollamaEmbeddingModel(AiProviderProperties aiProperties) {
    return ollamaEmbeddingModel(aiProperties, ObservationRegistry.NOOP);
  }

  @Bean(name = "ollamaEmbeddingModel")
  @ConditionalOnMissingBean(name = "ollamaEmbeddingModel")
  EmbeddingModel ollamaEmbeddingModel(
      AiProviderProperties aiProperties, ObservationRegistry observationRegistry) {
    OllamaEmbeddingModel model =
        OllamaEmbeddingModel.builder()
            .ollamaApi(OllamaApi.builder().baseUrl(aiProperties.embedding().baseUrl()).build())
            .options(
                OllamaEmbeddingOptions.builder().model(aiProperties.embedding().model()).build())
            .observationRegistry(observationRegistry)
            .build();
    model.setObservationConvention(
        SpringAiObservationConventions.embeddingModel(aiProperties.embedding().model()));
    return model;
  }

  @Bean
  SpringAiEmbeddingProviderRegistry providerRegistry(
      Map<String, EmbeddingModel> embeddingModels,
      SpringAiEmbeddingProperties properties,
      AiProviderProperties aiProperties,
      AiTraceRecorder traceRecorder) {
    return new SpringAiEmbeddingProviderRegistry(
        embeddingModels, properties, aiProperties.embeddingModelConfiguration(), traceRecorder);
  }

  SpringAiEmbeddingProviderRegistry providerRegistry(
      Map<String, EmbeddingModel> embeddingModels,
      SpringAiEmbeddingProperties properties,
      AiProviderProperties aiProperties) {
    return new SpringAiEmbeddingProviderRegistry(
        embeddingModels,
        properties,
        aiProperties.embeddingModelConfiguration(),
        NoopAiTraceRecorder.INSTANCE);
  }

  @Bean(name = "aiSemanticEmbeddingModel")
  @ConditionalOnMissingBean(name = "aiSemanticEmbeddingModel")
  EmbeddingService embeddingModel(
      SpringAiEmbeddingProviderRegistry registry,
      Optional<ModelExecutionScheduler> scheduler,
      AiExecutorProperties executionProperties) {
    return scheduler
        .map(admission -> embeddingModel(registry, admission, executionProperties))
        .orElseGet(() -> new EmbeddingModelSelector(registry.providers()));
  }

  EmbeddingService embeddingModel(
      SpringAiEmbeddingProviderRegistry registry,
      ModelExecutionScheduler scheduler,
      AiExecutorProperties executionProperties) {
    return new EmbeddingModelSelector(
        registry.providers(), scheduler, executionProperties.modelAdmissionTimeout());
  }

  EmbeddingService embeddingModel(SpringAiEmbeddingProviderRegistry registry) {
    return new EmbeddingModelSelector(registry.providers());
  }

  @Bean
  @ConditionalOnMissingBean
  SemanticQueryFactory semanticQueryFactory(EmbeddingService embeddings) {
    return new EmbeddingSemanticQueryFactory(embeddings);
  }
}
