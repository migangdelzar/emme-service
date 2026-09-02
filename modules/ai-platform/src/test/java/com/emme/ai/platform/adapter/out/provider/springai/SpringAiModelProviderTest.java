package com.emme.ai.platform.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SpringAiModelProviderTest {

  @Test
  void delegatesChatAndEmbeddingToTheSpringAiAdapters() {
    SpringAiChatModel chat = mock(SpringAiChatModel.class);
    SpringAiEmbeddingModel embedding = mock(SpringAiEmbeddingModel.class);
    when(chat.provider()).thenReturn("ollama");
    when(chat.complete("context", "hello")).thenReturn("hola");
    when(embedding.embed("faq")).thenReturn(List.of(0.25f, 0.75f));

    SpringAiModelProvider provider = new SpringAiModelProvider(chat, Optional.of(embedding));

    assertThat(provider.name()).isEqualTo("ollama");
    assertThat(provider.chat("context", "hello")).isEqualTo("hola");
    assertThat(provider.embed("faq")).containsExactly(0.25f, 0.75f);
    assertThat(provider.isMock()).isFalse();
  }

  @Test
  void representsAnUnsupportedEmbeddingCapabilityAsAnEmptyVector() {
    SpringAiChatModel chat = mock(SpringAiChatModel.class);
    when(chat.provider()).thenReturn("groq");

    SpringAiModelProvider provider = new SpringAiModelProvider(chat, Optional.empty());

    assertThat(provider.embed("faq")).isEmpty();
  }
}
