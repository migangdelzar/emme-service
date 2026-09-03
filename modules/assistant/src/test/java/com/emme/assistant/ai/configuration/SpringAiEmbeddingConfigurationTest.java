package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.platform.configuration.AiProviderProperties;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
                    "ollamaEmbeddingModel", "local", "ollama-embeddinggemma:300m"),
                new SpringAiEmbeddingProperties.Provider(
                    "openAiEmbeddingModel", "cloud", "ollama-embeddinggemma:300m")));

    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();
    SpringAiEmbeddingProviderRegistry registry =
        configuration.providerRegistry(
            Map.of("ollamaEmbeddingModel", local, "openAiEmbeddingModel", cloud),
            properties,
            aiProperties(2));
    EmbeddingModelPort embeddingModel = configuration.embeddingModel(registry);

    assertThat(embeddingModel.embed("faq"))
        .isEqualTo(new EmbeddingVector("ollama-embeddinggemma:300m", List.of(0.2f, 0.8f)));
    var invocationOrder = inOrder(local, cloud);
    invocationOrder.verify(local).embed("faq");
    invocationOrder.verify(cloud).embed("faq");
  }

  @Test
  void failsWhenAConfiguredProviderBeanIsMissing() {
    SpringAiEmbeddingProperties properties =
        new SpringAiEmbeddingProperties(
            true,
            List.of(
                new SpringAiEmbeddingProperties.Provider(
                    "ollamaEmbeddingModel", "local", "ollama-embeddinggemma:300m")));
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
                    "ollamaEmbeddingModel", "local", "ollama-embeddinggemma:300m")));
    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();
    EmbeddingModelPort embeddingModel =
        configuration.embeddingModel(
            configuration.providerRegistry(
                Map.of("ollamaEmbeddingModel", local), properties, aiProperties(2)));

    assertThatThrownBy(() -> embeddingModel.embed("faq"))
        .isInstanceOf(EmbeddingProviderUnavailableException.class);
  }

  @Test
  void propagatesAnInvalidProviderDimensionInsteadOfFallingBack() {
    EmbeddingModel local = mock(EmbeddingModel.class);
    EmbeddingModel cloud = mock(EmbeddingModel.class);
    when(local.embed("faq")).thenReturn(new float[] {0.2f});
    when(cloud.embed("faq")).thenReturn(new float[] {0.2f, 0.8f});
    SpringAiEmbeddingProperties properties =
        new SpringAiEmbeddingProperties(
            true,
            List.of(
                new SpringAiEmbeddingProperties.Provider(
                    "ollamaEmbeddingModel", "local", "ollama-embeddinggemma:300m"),
                new SpringAiEmbeddingProperties.Provider(
                    "openAiEmbeddingModel", "cloud", "ollama-embeddinggemma:300m")));
    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();
    EmbeddingModelPort embeddingModel =
        configuration.embeddingModel(
            configuration.providerRegistry(
                Map.of("ollamaEmbeddingModel", local, "openAiEmbeddingModel", cloud), properties,
                aiProperties(2)));

    assertThatThrownBy(() -> embeddingModel.embed("faq"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding dimension 1 does not match configured dimension 2");
    verifyNoInteractions(cloud);
  }

  @Test
  void rejectsAProviderThatWouldQueryADifferentEmbeddingIndexVersion() {
    EmbeddingModel local = mock(EmbeddingModel.class);
    SpringAiEmbeddingProperties properties =
        new SpringAiEmbeddingProperties(
            true,
            List.of(
                new SpringAiEmbeddingProperties.Provider(
                    "ollamaEmbeddingModel", "local", "different-embedding-space")));
    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();

    assertThatThrownBy(
            () ->
                configuration.providerRegistry(
                    Map.of("ollamaEmbeddingModel", local), properties, aiProperties(2)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding provider model version must match configured semantic index");
  }

  @Test
  void wiresTheTraceRecorderIntoTheConfiguredEmbeddingModelSelector() {
    EmbeddingModel local = mock(EmbeddingModel.class);
    when(local.embed("faq")).thenReturn(new float[] {0.2f, 0.8f});
    SpringAiEmbeddingProperties properties =
        new SpringAiEmbeddingProperties(
            true,
            List.of(
                new SpringAiEmbeddingProperties.Provider(
                    "ollamaEmbeddingModel", "local", "ollama-embeddinggemma:300m")));
    AiTraceRecorder recorder = mock(AiTraceRecorder.class);
    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();
    SpringAiEmbeddingProviderRegistry registry =
        configuration.providerRegistry(
            Map.of("ollamaEmbeddingModel", local), properties, aiProperties(2), recorder);

    AiExecutionContext context = context();
    AiExecutionContextScope.call(
        context, () -> configuration.embeddingModel(registry).embed("faq"));

    org.mockito.Mockito.verify(recorder).recordModelExecution(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createsTheLocalOllamaModelFromTheExistingProviderConfiguration() {
    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();

    assertThat(configuration.ollamaEmbeddingModel(aiProperties(1024)))
        .isInstanceOf(OllamaEmbeddingModel.class);
  }

  @Test
  void wiresTheExistingModelSchedulerIntoTheEmbeddingChain() {
    EmbeddingModel local = mock(EmbeddingModel.class);
    SpringAiEmbeddingProperties properties =
        new SpringAiEmbeddingProperties(
            true,
            List.of(
                new SpringAiEmbeddingProperties.Provider(
                    "ollamaEmbeddingModel", "local", "ollama-embeddinggemma:300m")));
    SpringAiEmbeddingConfiguration configuration = new SpringAiEmbeddingConfiguration();
    SpringAiEmbeddingProviderRegistry registry =
        configuration.providerRegistry(
            Map.of("ollamaEmbeddingModel", local), properties, aiProperties(2));

    assertThat(
            configuration.embeddingModel(
                registry, mock(ModelExecutionScheduler.class), new AiExecutorProperties(2, 1, 1)))
        .isInstanceOf(com.emme.assistant.ai.application.provider.EmbeddingModelSelector.class);
  }

  private static AiProviderProperties aiProperties(int dimension) {
    return new AiProviderProperties(
        "mock",
        null,
        new AiProviderProperties.EmbeddingConfig(
            "embeddinggemma:300m", "http://localhost:11434", null, dimension),
        true);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_CLIENT"), id, id, "trace-1", "idem-1");
  }
}
