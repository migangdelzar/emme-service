package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingProviderChainTest {

  private static final EmbeddingVector LOCAL_VECTOR =
      new EmbeddingVector("local-bge-m3", List.of(0.1f, 0.9f));
  private static final EmbeddingVector CLOUD_VECTOR =
      new EmbeddingVector("cloud-text-embedding", List.of(0.2f, 0.8f));

  @Test
  void returnsThePrimaryProviderResultWithoutCallingFallbacks() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelPort fallback = mock(EmbeddingModelPort.class);
    when(primary.embed("book Friday afternoon")).thenReturn(LOCAL_VECTOR);
    EmbeddingProviderChain chain =
        new EmbeddingProviderChain(
            List.of(
                new EmbeddingProviderChain.Provider("local", primary),
                new EmbeddingProviderChain.Provider("cloud", fallback)));

    assertThat(chain.embed("book Friday afternoon")).isEqualTo(LOCAL_VECTOR);

    verifyNoInteractions(fallback);
  }

  @Test
  void usesTheNextProviderOnlyWhenTheCurrentProviderIsUnavailable() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelPort fallback = mock(EmbeddingModelPort.class);
    when(primary.embed("quote this design"))
        .thenThrow(new EmbeddingProviderUnavailableException("local unavailable"));
    when(fallback.embed("quote this design")).thenReturn(CLOUD_VECTOR);
    EmbeddingProviderChain chain =
        new EmbeddingProviderChain(
            List.of(
                new EmbeddingProviderChain.Provider("local", primary),
                new EmbeddingProviderChain.Provider("cloud", fallback)));

    assertThat(chain.embed("quote this design")).isEqualTo(CLOUD_VECTOR);
  }

  @Test
  void doesNotFallbackWhenAProviderReturnsAnInvalidVector() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelPort fallback = mock(EmbeddingModelPort.class);
    when(primary.embed("quote this design"))
        .thenThrow(
            new IllegalStateException("Embedding dimension does not match configured dimension"));
    EmbeddingProviderChain chain =
        new EmbeddingProviderChain(
            List.of(
                new EmbeddingProviderChain.Provider("local", primary),
                new EmbeddingProviderChain.Provider("cloud", fallback)));

    assertThatThrownBy(() -> chain.embed("quote this design"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Embedding dimension does not match configured dimension");
    verifyNoInteractions(fallback);
  }

  @Test
  void rejectsBlankInputBeforeTouchingAnyProvider() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingProviderChain chain =
        new EmbeddingProviderChain(List.of(new EmbeddingProviderChain.Provider("local", primary)));

    assertThatThrownBy(() -> chain.embed(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Embedding text must not be blank");
    verifyNoInteractions(primary);
  }

  @Test
  void reportsWhenEveryProviderIsUnavailable() {
    EmbeddingModelPort primary = mock(EmbeddingModelPort.class);
    EmbeddingModelPort fallback = mock(EmbeddingModelPort.class);
    when(primary.embed("faq"))
        .thenThrow(new EmbeddingProviderUnavailableException("local unavailable"));
    when(fallback.embed("faq"))
        .thenThrow(new EmbeddingProviderUnavailableException("cloud unavailable"));
    EmbeddingProviderChain chain =
        new EmbeddingProviderChain(
            List.of(
                new EmbeddingProviderChain.Provider("local", primary),
                new EmbeddingProviderChain.Provider("cloud", fallback)));

    assertThatThrownBy(() -> chain.embed("faq"))
        .isInstanceOf(EmbeddingProviderUnavailableException.class)
        .hasMessage("All configured embedding providers are unavailable: local, cloud");
  }
}
