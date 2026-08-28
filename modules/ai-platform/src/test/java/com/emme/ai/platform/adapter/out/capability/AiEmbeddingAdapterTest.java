package com.emme.ai.platform.adapter.out.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiEmbeddingAdapterTest {

  private final AiModelProvider provider = mock(AiModelProvider.class);

  @Test
  void delegatesEmbeddingToTheConfiguredModelProvider() {
    when(provider.embed("gel manicure")).thenReturn(List.of(0.25f, 0.75f));

    List<Float> embedding = new AiEmbeddingAdapter(provider).embed("gel manicure");

    assertThat(embedding).containsExactly(0.25f, 0.75f);
  }
}
