package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.ai.contracts.image.CaptionImageUseCase;
import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.platform.adapter.out.provider.mock.MockEmbeddingService;
import com.emme.ai.platform.adapter.out.provider.mock.MockModelProvider;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiEmbeddingModel;
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
              assertThat(context).hasSingleBean(AiChatCompletion.class);
              assertThat(context).hasSingleBean(CaptionImageUseCase.class);
              assertThat(context).hasSingleBean(EmbeddingService.class);
            });
  }

  @Test
  void keepsTheDeterministicMockProviderActiveForMockConfiguration() {
    contextRunner
        .withPropertyValues("app.ai.provider=mock")
        .withUserConfiguration(MockModelProvider.class, MockEmbeddingService.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(AiChatCompletion.class);
              assertThat(context).hasSingleBean(CaptionImageUseCase.class);
              assertThat(context).hasSingleBean(EmbeddingService.class);
              assertThat(context.getBean(AiChatCompletion.class))
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

  @Test
  void wiresTheActiveGroqProviderThroughTheOpenAiCompatibleSpringAiAdapter() {
    contextRunner
        .withPropertyValues(
            "app.ai.provider=groq",
            "app.ai.chat.api-key=test-key",
            "app.ai.chat.model=llama-test",
            "app.ai.chat.base-url=http://localhost:9999")
        .withBean("groqChatClient", ChatClient.class, () -> mock(ChatClient.class))
        .run(
            context -> {
              assertThat(context).hasSingleBean(AiChatCompletion.class);
              assertThat(context).hasSingleBean(CaptionImageUseCase.class);
              assertThat(context).hasSingleBean(EmbeddingService.class);
              assertThat(context.getBean(AiChatCompletion.class))
                  .isInstanceOf(
                      com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatCompletion
                          .class);
              assertThat(context.getBean("groqChatClient")).isInstanceOf(ChatClient.class);
            });
  }
}
