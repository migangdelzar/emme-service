package com.emme.ai.platform.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    assertThat(AiExecutionContextScope.call(context(), () -> provider.chat("context", "hello")))
        .isEqualTo("hola");
    assertThat(AiExecutionContextScope.call(context(), () -> provider.embed("faq")))
        .containsExactly(0.25f, 0.75f);
    assertThat(provider.isMock()).isFalse();
  }

  @Test
  void exposesTheCanonicalChatCapabilityThroughTheCompatibilityProvider() {
    SpringAiChatModel chat = mock(SpringAiChatModel.class);
    when(chat.provider()).thenReturn("ollama");
    when(chat.complete("context", "hello")).thenReturn("hola");

    SpringAiModelProvider provider = new SpringAiModelProvider(chat, Optional.empty());

    assertThat(AiExecutionContextScope.call(context(), () -> provider.complete("context", "hello")))
        .isEqualTo("hola");
  }

  @Test
  void representsAnUnsupportedEmbeddingCapabilityAsAnEmptyVector() {
    SpringAiChatModel chat = mock(SpringAiChatModel.class);
    when(chat.provider()).thenReturn("groq");

    SpringAiModelProvider provider = new SpringAiModelProvider(chat, Optional.empty());

    assertThat(AiExecutionContextScope.call(context(), () -> provider.embed("faq"))).isEmpty();
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
