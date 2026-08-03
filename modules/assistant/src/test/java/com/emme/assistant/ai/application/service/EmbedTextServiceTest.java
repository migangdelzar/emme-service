package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ModelProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbedTextServiceTest {

  private final ModelProvider provider = mock(ModelProvider.class);

  @Test
  void delegatesEmbeddingToTheConfiguredModelProvider() {
    when(provider.embed("gel manicure")).thenReturn(List.of(0.25f, 0.75f));

    List<Float> embedding = new EmbedTextService(provider).embed("gel manicure");

    assertThat(embedding).containsExactly(0.25f, 0.75f);
  }
}
