package com.emme.ai.platform.configuration;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiEmbeddingModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiModelProvider;
import com.emme.ai.platform.model.BoundedModelExecutionScheduler;
import io.micrometer.observation.ObservationRegistry;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for provider configuration and transport dependencies. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({AiProviderProperties.class, ModelAdmissionProperties.class})
public class AiProviderConfiguration {

  ChatClient ollamaChatClient(AiProviderProperties properties) {
    return ollamaChatClient(properties, ObservationRegistry.NOOP);
  }

  @Bean(name = "ollamaChatClient")
  @ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
  @ConditionalOnMissingBean(name = "ollamaChatClient")
  ChatClient ollamaChatClient(
      AiProviderProperties properties, ObservationRegistry observationRegistry) {
    ChatModel model =
        OllamaChatModel.builder()
            .ollamaApi(OllamaApi.builder().baseUrl(properties.chat().baseUrl()).build())
            .options(OllamaChatOptions.builder().model(properties.chat().model()).build())
            .observationRegistry(observationRegistry)
            .build();
    return ChatClient.create(model, observationRegistry);
  }

  EmbeddingModel ollamaEmbeddingModel(AiProviderProperties properties) {
    return ollamaEmbeddingModel(properties, ObservationRegistry.NOOP);
  }

  @Bean(name = "ollamaEmbeddingModel")
  @ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")
  @ConditionalOnMissingBean(name = "ollamaEmbeddingModel")
  EmbeddingModel ollamaEmbeddingModel(
      AiProviderProperties properties, ObservationRegistry observationRegistry) {
    OllamaEmbeddingModel model =
        OllamaEmbeddingModel.builder()
            .ollamaApi(OllamaApi.builder().baseUrl(properties.embedding().baseUrl()).build())
            .options(OllamaEmbeddingOptions.builder().model(properties.embedding().model()).build())
            .observationRegistry(observationRegistry)
            .build();
    model.setObservationConvention(
        SpringAiObservationConventions.embeddingModel(properties.embedding().model()));
    return model;
  }

  ChatClient groqChatClient(AiProviderProperties properties) {
    return groqChatClient(properties, ObservationRegistry.NOOP);
  }

  @Bean(name = "groqChatClient")
  @ConditionalOnProperty(name = "app.ai.provider", havingValue = "groq")
  @ConditionalOnMissingBean(name = "groqChatClient")
  ChatClient groqChatClient(
      AiProviderProperties properties, ObservationRegistry observationRegistry) {
    ChatModel model =
        OpenAiChatModel.builder()
            .options(
                OpenAiChatOptions.builder()
                    .baseUrl(properties.chat().baseUrl())
                    .apiKey(properties.chat().apiKey())
                    .model(properties.chat().model())
                    .build())
            .observationRegistry(observationRegistry)
            .build();
    return ChatClient.create(model, observationRegistry);
  }

  @Bean
  @ConditionalOnBean(name = {"ollamaChatClient", "ollamaEmbeddingModel"})
  @ConditionalOnMissingBean(AiModelProvider.class)
  AiModelProvider ollamaModelProvider(
      @Qualifier("ollamaChatClient") ChatClient chatClient,
      @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
      AiProviderProperties properties) {
    return new SpringAiModelProvider(
        new SpringAiChatModel(chatClient, properties.provider(), properties.chat().model()),
        Optional.of(
            new SpringAiEmbeddingModel(
                embeddingModel,
                properties.provider(),
                properties.embedding().modelVersion(),
                properties.embedding().dimension())));
  }

  @Bean
  @ConditionalOnBean(name = "groqChatClient")
  @ConditionalOnMissingBean(AiModelProvider.class)
  AiModelProvider groqModelProvider(
      @Qualifier("groqChatClient") ChatClient chatClient, AiProviderProperties properties) {
    return new SpringAiModelProvider(
        new SpringAiChatModel(chatClient, properties.provider(), properties.chat().model()),
        Optional.empty());
  }

  @Bean
  @ConditionalOnMissingBean(ModelExecutionScheduler.class)
  ModelExecutionScheduler modelExecutionScheduler(ModelAdmissionProperties properties) {
    return new BoundedModelExecutionScheduler(properties.profile());
  }
}
