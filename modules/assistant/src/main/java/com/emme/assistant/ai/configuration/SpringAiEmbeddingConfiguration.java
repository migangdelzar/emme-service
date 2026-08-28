package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.provider.EmbeddingProviderChain;
import java.util.Map;
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

  @Bean(name = "ollamaEmbeddingModel")
  @ConditionalOnMissingBean(name = "ollamaEmbeddingModel")
  EmbeddingModel ollamaEmbeddingModel(AiProperties aiProperties) {
    return OllamaEmbeddingModel.builder()
        .ollamaApi(OllamaApi.builder().baseUrl(aiProperties.embedding().baseUrl()).build())
        .options(OllamaEmbeddingOptions.builder().model(aiProperties.embedding().model()).build())
        .build();
  }

  @Bean
  SpringAiEmbeddingProviderRegistry providerRegistry(
      Map<String, EmbeddingModel> embeddingModels,
      SpringAiEmbeddingProperties properties,
      AiProperties aiProperties) {
    return new SpringAiEmbeddingProviderRegistry(
        embeddingModels, properties, aiProperties.embeddingDimension());
  }

  @Bean(name = "aiSemanticEmbeddingModel")
  @ConditionalOnMissingBean(name = "aiSemanticEmbeddingModel")
  EmbeddingModelPort embeddingModel(SpringAiEmbeddingProviderRegistry registry) {
    return new EmbeddingProviderChain(registry.providers());
  }
}
