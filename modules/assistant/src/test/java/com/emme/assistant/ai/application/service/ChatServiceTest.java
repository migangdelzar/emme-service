package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

  @Test
  void rejectsChatWithoutBackendAiExecutionContext() {
    AiModelProvider model = mock(AiModelProvider.class);
    ChatService service = new ChatService(model, Optional.empty());

    assertThatThrownBy(() -> service.chat("", "hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void returnsAHighConfidenceCacheHitWithoutCallingTheModel() {
    AiModelProvider model = mock(AiModelProvider.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?")).thenReturn(Optional.of("Open today."));
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");

    verifyNoInteractions(model);
  }

  @Test
  void storesAProviderResponseAfterASemanticCacheMiss() {
    AiModelProvider model = mock(AiModelProvider.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?")).thenReturn(Optional.empty());
    when(model.chat("", "What are your hours?")).thenReturn("Open today.");
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");

    verify(cache).store("", "What are your hours?", "Open today.");
  }

  @Test
  void preservesExistingModelBehaviorWhenSemanticCachingIsDisabled() {
    AiModelProvider model = mock(AiModelProvider.class);
    when(model.chat("context", "hello")).thenReturn("response");
    ChatService service = new ChatService(model, Optional.empty());

    assertThat(inContext(() -> service.chat("context", "hello"))).isEqualTo("response");
    verify(model).chat("context", "hello");
  }

  @Test
  void fallsBackToTheNormalModelWhenSemanticCacheInfrastructureFails() {
    AiModelProvider model = mock(AiModelProvider.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?"))
        .thenThrow(new IllegalStateException("database unavailable"));
    when(model.chat("", "What are your hours?")).thenReturn("Open today.");
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");
    verify(model).chat("", "What are your hours?");
  }

  @Test
  void recordsTheReasonWhenSemanticCacheFailureFallsBackToTheModel() {
    AiModelProvider model = mock(AiModelProvider.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    SemanticMetrics metrics = mock(SemanticMetrics.class);
    when(cache.lookup("", "What are your hours?"))
        .thenThrow(new IllegalStateException("database unavailable"));
    when(model.chat("", "What are your hours?")).thenReturn("Open today.");
    ChatService service =
        new ChatService(
            model,
            Optional.of(cache),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Duration.ofSeconds(5),
            metrics);

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");

    verify(metrics).recordFallback("chat", "semantic_cache_failure");
  }

  @Test
  void propagatesSecurityFailuresFromSemanticCacheInsteadOfFallingBack() {
    AiModelProvider model = mock(AiModelProvider.class);
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
    AiModelProvider model = mock(AiModelProvider.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    when(cache.lookup("", "What are your hours?")).thenReturn(Optional.empty());
    when(model.chat("", "What are your hours?")).thenReturn("Open today.");
    when(cache.store("", "What are your hours?", "Open today."))
        .thenThrow(new IllegalStateException("database unavailable"));
    ChatService service = new ChatService(model, Optional.of(cache));

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");
  }

  @Test
  void returnsAProactiveToolResultBeforeCacheOrModelExecution() {
    AiModelProvider model = mock(AiModelProvider.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    ProactiveToolRouter router = mock(ProactiveToolRouter.class);
    when(router.route("what services do you have?"))
        .thenReturn(Optional.of(new AiToolResult("getSalonServices", "services", true)));
    ChatService service =
        new ChatService(model, Optional.of(cache), Optional.empty(), Optional.of(router));

    assertThat(inContext(() -> service.chat("", "what services do you have?")))
        .isEqualTo("services");

    verifyNoInteractions(model, cache);
  }

  @Test
  void admitsLegacyProviderExecutionThroughTheSharedModelScheduler() {
    AiModelProvider model = mock(AiModelProvider.class);
    ModelExecutionScheduler scheduler = mock(ModelExecutionScheduler.class);
    when(model.chat("", "hello")).thenReturn("response");
    when(scheduler.execute(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> invocation.getArgument(3, java.util.concurrent.Callable.class).call());
    AiExecutionContext context = context();
    ChatService service =
        new ChatService(
            model,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(scheduler),
            Duration.ofSeconds(2));

    assertThat(AiExecutionContextScope.call(context, () -> service.chat("", "hello")))
        .isEqualTo("response");

    verify(scheduler)
        .execute(eq(ModelCapability.GENERATION), eq(context), eq(Duration.ofSeconds(2)), any());
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
