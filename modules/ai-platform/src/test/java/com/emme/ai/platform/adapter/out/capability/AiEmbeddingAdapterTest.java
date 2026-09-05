package com.emme.ai.platform.adapter.out.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.semantic.EmbeddingModelConfiguration;
import com.emme.ai.platform.configuration.AiProviderProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiEmbeddingAdapterTest {

  private final AiModelProvider provider = mock(AiModelProvider.class);

  @Test
  void delegatesEmbeddingToTheConfiguredModelProvider() {
    when(provider.embed("gel manicure")).thenReturn(List.of(0.25f, 0.75f));

    var embedding =
        new AiEmbeddingAdapter(provider, new EmbeddingModelConfiguration("embeddinggemma", "v1", 2))
            .embed("gel manicure");

    assertThat(embedding.values()).containsExactly(0.25f, 0.75f);
    assertThat(embedding.model().modelName()).isEqualTo("embeddinggemma");
  }

  @Test
  void canBeConstructedFromManagedProviderProperties() {
    when(provider.embed("gel manicure")).thenReturn(List.of(0.25f, 0.75f));

    var properties =
        new AiProviderProperties(
            null,
            null,
            new AiProviderProperties.EmbeddingConfig(
                "embeddinggemma:300m", "http://localhost", null, 2, "v1"),
            false);

    var embedding = new AiEmbeddingAdapter(provider, properties).embed("gel manicure");

    assertThat(embedding.model().dimension()).isEqualTo(2);
  }
}
