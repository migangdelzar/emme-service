package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.service.EmbeddingVector;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;

class SpringAiEmbeddingConfigurationTest {

  @Test
  void buildsTheConfiguredProviderOrderAroundNamedSpringAiModels() {
    EmbeddingModel local = mock(EmbeddingModel.class);
    EmbeddingModel cloud = mock(EmbeddingModel.class);
    when(local.embed("faq")).thenThrow(new RuntimeException("local unavailable"));
    when(cloud.embed("faq")).thenReturn(new float[] {0.2f, 0.8f});
    SpringAiEmbeddingProperties properties =
        new SpringAiEmbeddingProperties(
            true,
            List.of(
                new SpringAiEmbeddingProperties.Provider(
                    "ollamaEmbeddingModel", "local", "ollama-bge-m3"),
                new SpringAiEmbeddingProperties.Provider(
                    "openAiEmbeddingModel", "cloud", "openai-text-embedding")));

    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();
    SpringAiEmbeddingProviderRegistry registry =
        configuration.providerRegistry(
            Map.of("ollamaEmbeddingModel", local, "openAiEmbeddingModel", cloud),
            properties,
            aiProperties(2));
    EmbeddingModelPort embeddingModel = configuration.embeddingModel(registry);

    assertThat(embeddingModel.embed("faq"))
        .isEqualTo(new EmbeddingVector("openai-text-embedding", List.of(0.2f, 0.8f)));
  }

  @Test
  void failsWhenAConfiguredProviderBeanIsMissing() {
    SpringAiEmbeddingProperties properties =
        new SpringAiEmbeddingProperties(
            true,
            List.of(
                new SpringAiEmbeddingProperties.Provider(
                    "ollamaEmbeddingModel", "local", "ollama-bge-m3")));
    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();

    assertThatThrownBy(() -> configuration.providerRegistry(Map.of(), properties, aiProperties(2)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No Spring AI embedding model bean configured for provider 'local'");
  }

  @Test
  void doesNotTreatProviderFailureAsAValidEmbedding() {
    EmbeddingModel local = mock(EmbeddingModel.class);
    when(local.embed("faq")).thenThrow(new RuntimeException("connection refused"));
    SpringAiEmbeddingProperties properties =
        new SpringAiEmbeddingProperties(
            true,
            List.of(
                new SpringAiEmbeddingProperties.Provider(
                    "ollamaEmbeddingModel", "local", "ollama-bge-m3")));
    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();
    EmbeddingModelPort embeddingModel =
        configuration.embeddingModel(
            configuration.providerRegistry(
                Map.of("ollamaEmbeddingModel", local), properties, aiProperties(2)));

    assertThatThrownBy(() -> embeddingModel.embed("faq"))
        .isInstanceOf(EmbeddingProviderUnavailableException.class);
  }

  @Test
  void createsTheLocalOllamaModelFromTheExistingProviderConfiguration() {
    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();

    assertThat(configuration.ollamaEmbeddingModel(aiProperties(1024)))
        .isInstanceOf(OllamaEmbeddingModel.class);
  }

  private static AiProperties aiProperties(int dimension) {
    return new AiProperties(
        "mock",
        null,
        new AiProperties.EmbeddingConfig("bge-m3", "http://localhost:11434", null, dimension),
        true);
  }
}
