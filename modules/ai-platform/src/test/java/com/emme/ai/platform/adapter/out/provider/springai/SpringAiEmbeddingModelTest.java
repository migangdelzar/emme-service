package com.emme.ai.platform.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

class SpringAiEmbeddingModelTest {

  @Test
  void delegatesEmbeddingAndExposesTheConfiguredProviderIdentity() {
    EmbeddingModel delegate = mock(EmbeddingModel.class);
    when(delegate.embed("faq")).thenReturn(new float[] {0.25f, 0.75f});
    SpringAiEmbeddingModel model =
        new SpringAiEmbeddingModel(delegate, "ollama", "embedding-v1", 2);

    assertThat(model.embed("faq")).containsExactly(0.25f, 0.75f);
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

    assertThatThrownBy(() -> model.embed("faq")).isSameAs(failure);
  }

  @Test
  void rejectsAProviderVectorWithAnInvalidConfiguredDimension() {
    EmbeddingModel delegate = mock(EmbeddingModel.class);
    when(delegate.embed("faq")).thenReturn(new float[] {0.25f});
    SpringAiEmbeddingModel model =
        new SpringAiEmbeddingModel(delegate, "ollama", "embedding-v1", 2);

    assertThatThrownBy(() -> model.embed("faq"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding dimension 1 does not match configured dimension 2");
  }
}
