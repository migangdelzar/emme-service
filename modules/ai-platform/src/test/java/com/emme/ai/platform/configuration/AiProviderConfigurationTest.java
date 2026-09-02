package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiProviderConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(AiProviderConfiguration.class)
          .withBean("ollamaChatClient", ChatClient.class, () -> mock(ChatClient.class))
          .withBean("ollamaEmbeddingModel", EmbeddingModel.class, () -> mock(EmbeddingModel.class));

  @Test
  void doesNotCreateUnscopedSpringAiAdaptersForProviderOwnedBeans() {
    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(SpringAiChatModel.class);
          assertThat(context).doesNotHaveBean(SpringAiEmbeddingModel.class);
        });
  }
}
