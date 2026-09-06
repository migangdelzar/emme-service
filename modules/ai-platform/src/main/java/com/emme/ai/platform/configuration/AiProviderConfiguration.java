package com.emme.ai.platform.configuration;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.image.CaptionImageUseCase;
import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.contracts.semantic.DistanceMetric;
import com.emme.ai.contracts.semantic.EmbeddingModelVersion;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatCompletion;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiEmbeddingModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiVisionModel;
import com.emme.ai.platform.model.BoundedModelExecutionScheduler;
import io.micrometer.observation.ObservationRegistry;
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
import org.springframework.context.annotation.Primary;

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
  @ConditionalOnMissingBean(AiChatCompletion.class)
  AiChatCompletion ollamaChatCompletion(
      @Qualifier("ollamaChatClient") ChatClient chatClient, AiProviderProperties properties) {
    return new SpringAiChatCompletion(
        new SpringAiChatModel(chatClient, properties.provider(), properties.chat().model()));
  }

  @Bean
  @ConditionalOnBean(name = "groqChatClient")
  @ConditionalOnMissingBean(AiChatCompletion.class)
  AiChatCompletion groqChatCompletion(
      @Qualifier("groqChatClient") ChatClient chatClient, AiProviderProperties properties) {
    return new SpringAiChatCompletion(
        new SpringAiChatModel(chatClient, properties.provider(), properties.chat().model()));
  }

  @Bean
  @ConditionalOnBean(name = {"ollamaChatClient", "ollamaEmbeddingModel"})
  @ConditionalOnMissingBean(CaptionImageUseCase.class)
  CaptionImageUseCase ollamaCaptionImage(@Qualifier("ollamaChatClient") ChatClient chatClient) {
    return new SpringAiVisionModel(chatClient);
  }

  @Bean
  @ConditionalOnProperty(name = "app.ai.provider", havingValue = "groq")
  @ConditionalOnMissingBean(CaptionImageUseCase.class)
  CaptionImageUseCase unsupportedGroqCaptionImage(AiProviderProperties properties) {
    return imageBase64 -> {
      throw new UnsupportedOperationException(
          "Provider '" + properties.provider() + "' does not support vision captioning");
    };
  }

  @Bean
  @Primary
  @ConditionalOnBean(name = "ollamaEmbeddingModel")
  @ConditionalOnMissingBean(EmbeddingService.class)
  EmbeddingService ollamaEmbeddingService(
      @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
      AiProviderProperties properties) {
    return new SpringAiEmbeddingModel(
        embeddingModel,
        properties.provider(),
        new EmbeddingModelVersion(
            properties.embedding().model(),
            properties.embeddingModelVersion(),
            properties.embeddingDimension(),
            DistanceMetric.COSINE,
            "query-v1"));
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "app.ai.provider", havingValue = "groq")
  @ConditionalOnMissingBean(EmbeddingService.class)
  EmbeddingService unsupportedGroqEmbedding(AiProviderProperties properties) {
    return text -> {
      throw new UnsupportedOperationException(
          "Provider '" + properties.provider() + "' does not support embeddings");
    };
  }

  @Bean
  @ConditionalOnMissingBean(ModelExecutionScheduler.class)
  ModelExecutionScheduler modelExecutionScheduler(ModelAdmissionProperties properties) {
    return new BoundedModelExecutionScheduler(properties.profile());
  }
}
