package com.emme.ai.platform.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

class SpringAiEmbeddingModelTest {

  @Test
  void delegatesEmbeddingAndExposesTheConfiguredProviderIdentity() {
    EmbeddingModel delegate = mock(EmbeddingModel.class);
    when(delegate.embed("faq")).thenReturn(new float[] {0.25f, 0.75f});
    SpringAiEmbeddingModel model =
        new SpringAiEmbeddingModel(delegate, "ollama", "embedding-v1", 2);

    assertThat(AiExecutionContextScope.call(context(), () -> model.embed("faq")))
        .containsExactly(0.25f, 0.75f);
    assertThat(model.provider()).isEqualTo("ollama");
    assertThat(model.modelVersion()).isEqualTo("embedding-v1");
  }

  @Test
  void propagatesProviderFailuresWithoutReproducingTransportHandling() {
    EmbeddingModel delegate = mock(EmbeddingModel.class);
    RuntimeException failure = new RuntimeException("connection refused");
    when(delegate.embed("faq")).thenThrow(failure);
    SpringAiEmbeddingModel model =
        new SpringAiEmbeddingModel(delegate, "ollama", "embedding-v1", 2);

    assertThatThrownBy(() -> AiExecutionContextScope.call(context(), () -> model.embed("faq")))
        .isSameAs(failure);
  }

  @Test
  void rejectsAProviderVectorWithAnInvalidConfiguredDimension() {
    EmbeddingModel delegate = mock(EmbeddingModel.class);
    when(delegate.embed("faq")).thenReturn(new float[] {0.25f});
    SpringAiEmbeddingModel model =
        new SpringAiEmbeddingModel(delegate, "ollama", "embedding-v1", 2);

    assertThatThrownBy(() -> AiExecutionContextScope.call(context(), () -> model.embed("faq")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding dimension 1 does not match configured dimension 2");
  }

  @Test
  void rejectsEmbeddingWhenTheBackendAiContextIsMissing() {
    EmbeddingModel delegate = mock(EmbeddingModel.class);
    SpringAiEmbeddingModel model =
        new SpringAiEmbeddingModel(delegate, "ollama", "embedding-v1", 2);

    assertThatThrownBy(() -> model.embed("faq"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-1",
        "idempotency-1");
  }
}
