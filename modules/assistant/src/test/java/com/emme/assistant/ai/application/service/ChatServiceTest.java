package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

  @Test
  void rejectsChatWithoutBackendAiExecutionContext() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    ChatService service = new ChatService(model, Optional.empty());

    assertThatThrownBy(() -> service.chat("", "hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void returnsAHighConfidenceCacheHitWithoutCallingTheModel() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?")).thenReturn(Optional.of("Open today."));
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");

    verifyNoInteractions(model);
  }

  @Test
  void storesAProviderResponseAfterASemanticCacheMiss() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?")).thenReturn(Optional.empty());
    when(model.complete("", "What are your hours?")).thenReturn("Open today.");
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");

    verify(cache).store("", "What are your hours?", "Open today.");
  }

  @Test
  void preservesExistingModelBehaviorWhenSemanticCachingIsDisabled() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    when(model.complete("context", "hello")).thenReturn("response");
    ChatService service = new ChatService(model, Optional.empty());

    assertThat(inContext(() -> service.chat("context", "hello"))).isEqualTo("response");
    verify(model).complete("context", "hello");
  }

  @Test
  void fallsBackToTheNormalModelWhenSemanticCacheInfrastructureFails() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?"))
        .thenThrow(new IllegalStateException("database unavailable"));
    when(model.complete("", "What are your hours?")).thenReturn("Open today.");
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");
    verify(model).complete("", "What are your hours?");
  }

  @Test
  void recordsTheReasonWhenSemanticCacheFailureFallsBackToTheModel() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    SemanticMetrics metrics = mock(SemanticMetrics.class);
    when(cache.lookup("", "What are your hours?"))
        .thenThrow(new IllegalStateException("database unavailable"));
    when(model.complete("", "What are your hours?")).thenReturn("Open today.");
    ChatService service = new ChatService(model, Optional.of(cache), Optional.empty(), metrics);

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");

    verify(metrics).recordFallback("chat", "semantic_cache_failure");
  }

  @Test
  void propagatesSecurityFailuresFromSemanticCacheInsteadOfFallingBack() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    SecurityException failure = new SecurityException("tenant access denied");
    when(cache.lookup("", "What are your hours?")).thenThrow(failure);
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThatThrownBy(() -> inContext(() -> service.chat("", "What are your hours?")))
        .isSameAs(failure);
    verifyNoInteractions(model);
  }

  @Test
  void keepsTheModelResponseWhenSemanticCacheWriteInfrastructureFails() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?")).thenReturn(Optional.empty());
    when(model.complete("", "What are your hours?")).thenReturn("Open today.");
    when(cache.store("", "What are your hours?", "Open today."))
        .thenThrow(new IllegalStateException("database unavailable"));
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");
  }

  @Test
  void returnsAProactiveToolResultBeforeCacheOrModelExecution() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    ProactiveToolRouter router = mock(ProactiveToolRouter.class);
    when(router.route("what services do you have?"))
        .thenReturn(Optional.of(new AiToolResult("getSalonServices", "services", true)));
    ChatService service = new ChatService(model, Optional.of(cache), Optional.of(router));

    assertThat(inContext(() -> service.chat("", "what services do you have?")))
        .isEqualTo("services");

    verifyNoInteractions(model, cache);
  }

  @Test
  void executesThroughTheRequiredCanonicalChatPort() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    when(model.complete("", "hello")).thenReturn("response");
    ChatService service = new ChatService(model, Optional.empty());

    assertThat(inContext(() -> service.chat("", "hello"))).isEqualTo("response");

    verify(model).complete("", "hello");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace",
        "idempotency");
  }

  private static <T> T inContext(java.util.function.Supplier<T> action) {
    return AiExecutionContextScope.call(context(), action::get);
  }
}
