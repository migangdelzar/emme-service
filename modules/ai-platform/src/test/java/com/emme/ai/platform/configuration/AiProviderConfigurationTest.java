package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.platform.adapter.out.provider.mock.MockModelProvider;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiEmbeddingModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiModelProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiProviderConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(AiProviderConfiguration.class);

  @Test
  void wiresTheActiveOllamaProviderThroughSpringAiAdapters() {
    contextRunner
        .withPropertyValues("app.ai.provider=ollama")
        .withBean("ollamaChatClient", ChatClient.class, () -> mock(ChatClient.class))
        .withBean("ollamaEmbeddingModel", EmbeddingModel.class, () -> mock(EmbeddingModel.class))
        .run(
            context -> {
              assertThat(context).hasSingleBean(AiModelProvider.class);
              assertThat(context.getBean(AiModelProvider.class))
                  .isInstanceOf(SpringAiModelProvider.class);
            });
  }

  @Test
  void keepsTheDeterministicMockProviderActiveForMockConfiguration() {
    contextRunner
        .withPropertyValues("app.ai.provider=mock")
        .withUserConfiguration(MockModelProvider.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(AiModelProvider.class);
              assertThat(context.getBean(AiModelProvider.class))
                  .isInstanceOf(MockModelProvider.class);
            });
  }

  @Test
  void doesNotCreateUnscopedSpringAiAdaptersForProviderOwnedBeans() {
    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(SpringAiChatModel.class);
          assertThat(context).doesNotHaveBean(SpringAiEmbeddingModel.class);
        });
  }
}
