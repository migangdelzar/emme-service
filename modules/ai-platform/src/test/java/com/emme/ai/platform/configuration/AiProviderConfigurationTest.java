package com.emme.ai.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModel;
import com.emme.ai.platform.adapter.out.provider.springai.SpringAiEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;

class AiProviderConfigurationTest {

  @Test
  void createsTheSpringAiChatAdapterFromExistingProviderConfiguration() {
    AiProviderProperties properties =
        new AiProviderProperties(
            "ollama",
            new AiProviderProperties.ProviderConfig("gemma-v1", "http://localhost:11434", null),
            null,
            false);

    SpringAiChatModel model =
        new AiProviderConfiguration().springAiChatModel(mock(ChatClient.class), properties);

    assertThat(model.provider()).isEqualTo("ollama");
    assertThat(model.modelVersion()).isEqualTo("gemma-v1");
  }

  @Test
  void createsTheSpringAiEmbeddingAdapterFromExistingProviderConfiguration() {
    AiProviderProperties properties =
        new AiProviderProperties(
            "ollama",
            null,
            new AiProviderProperties.EmbeddingConfig(
                "embedding-v1", "http://localhost:11434", null, 2),
            false);

    SpringAiEmbeddingModel model =
        new AiProviderConfiguration()
            .springAiEmbeddingModel(mock(EmbeddingModel.class), properties);

    assertThat(model.provider()).isEqualTo("ollama");
    assertThat(model.modelVersion()).isEqualTo("embedding-v1");
    assertThat(model.dimension()).isEqualTo(2);
  }
}
