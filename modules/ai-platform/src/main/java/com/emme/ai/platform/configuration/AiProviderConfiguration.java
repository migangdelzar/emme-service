package com.emme.ai.platform.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiEmbeddingModel;
import com.emme.ai.platform.model.BoundedModelExecutionScheduler;
import okhttp3.OkHttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for provider configuration and transport dependencies. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({AiProviderProperties.class, ModelAdmissionProperties.class})
public class AiProviderConfiguration {

  @Bean
  AiProviderHttpClient aiProviderHttpClient() {
    return new AiProviderHttpClient(new OkHttpClient());
  }

  @Bean(name = "aiSpringChatModel")
  @ConditionalOnMissingBean(name = "aiSpringChatModel")
  @ConditionalOnBean(name = "ollamaChatClient")
  SpringAiChatModel springAiChatModel(
      @Qualifier("ollamaChatClient") ChatClient client, AiProviderProperties properties) {
    return new SpringAiChatModel(client, properties.provider(), properties.chat().model());
  }

  @Bean(name = "aiSpringEmbeddingModel")
  @ConditionalOnMissingBean(name = "aiSpringEmbeddingModel")
  @ConditionalOnBean(name = "ollamaEmbeddingModel")
  SpringAiEmbeddingModel springAiEmbeddingModel(
      @Qualifier("ollamaEmbeddingModel") EmbeddingModel model, AiProviderProperties properties) {
    return new SpringAiEmbeddingModel(
        model,
        properties.provider(),
        properties.embedding().modelVersion(),
        properties.embedding().dimension());
  }

  @Bean
  @ConditionalOnMissingBean(ModelExecutionScheduler.class)
  ModelExecutionScheduler modelExecutionScheduler(ModelAdmissionProperties properties) {
    return new BoundedModelExecutionScheduler(properties.profile());
  }
}
