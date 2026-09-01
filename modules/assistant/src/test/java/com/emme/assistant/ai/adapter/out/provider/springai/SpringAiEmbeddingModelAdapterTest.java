package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.emme.ai.contracts.semantic.EmbeddingModelConfiguration;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;

class SpringAiEmbeddingModelAdapterTest {

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void rejectsRawVectorsWithAnUnexpectedDimensionBeforeSpringAiUsesThem() {
    EmbeddingModelPort embeddings = mock(EmbeddingModelPort.class);
    EmbeddingModel adapter =
        new SpringAiEmbeddingModelAdapter(
            embeddings, new EmbeddingModelConfiguration("embeddinggemma:300m", "v1", 2));
    EmbeddingRequest request = new EmbeddingRequest((List) List.of(new float[] {0.25f}), null);

    assertThatThrownBy(() -> adapter.call(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding dimensions must match configured model");
  }
}
