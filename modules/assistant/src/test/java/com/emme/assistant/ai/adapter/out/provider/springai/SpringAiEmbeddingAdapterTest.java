package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

class SpringAiEmbeddingAdapterTest {

  @Test
  void convertsSpringAiEmbeddingIntoTheApplicationVectorContract() {
    EmbeddingModel model = mock(EmbeddingModel.class);
    when(model.embed("book Friday afternoon")).thenReturn(new float[] {0.25f, 0.75f});
    EmbeddingModelPort adapter =
        new SpringAiEmbeddingAdapter(model, "ollama-embeddinggemma:300m", 2);

    EmbeddingVector result = adapter.embed("book Friday afternoon");

    assertThat(result.modelVersion()).isEqualTo("ollama-embeddinggemma:300m");
    assertThat(result.values()).containsExactly(0.25f, 0.75f);
  }

  @Test
  void rejectsAnEmbeddingWithAnUnexpectedDimensionBeforePersistence() {
    EmbeddingModel model = mock(EmbeddingModel.class);
    when(model.embed("quote this design")).thenReturn(new float[] {0.25f});
    EmbeddingModelPort adapter =
        new SpringAiEmbeddingAdapter(model, "ollama-embeddinggemma:300m", 2);

    assertThatThrownBy(() -> adapter.embed("quote this design"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Embedding dimension 1 does not match configured dimension 2");
  }

  @Test
  void rejectsBlankInputBeforeCallingTheModel() {
    EmbeddingModel model = mock(EmbeddingModel.class);
    EmbeddingModelPort adapter =
        new SpringAiEmbeddingAdapter(model, "ollama-embeddinggemma:300m", 2);

    assertThatThrownBy(() -> adapter.embed(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding text must not be blank");
  }

  @Test
  void translatesSpringAiProviderFailuresIntoAnExplicitlyRetryableFailure() {
    EmbeddingModel model = mock(EmbeddingModel.class);
    when(model.embed("faq")).thenThrow(new RuntimeException("connection refused"));
    EmbeddingModelPort adapter =
        new SpringAiEmbeddingAdapter(model, "ollama-embeddinggemma:300m", 2);

    assertThatThrownBy(() -> adapter.embed("faq"))
        .isInstanceOf(EmbeddingProviderUnavailableException.class)
        .hasMessage("Spring AI embedding provider failed: connection refused");
  }
}
