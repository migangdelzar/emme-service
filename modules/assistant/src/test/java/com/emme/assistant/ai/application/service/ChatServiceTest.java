package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.assistant.ai.application.guardrail.InputGuard;
import com.emme.assistant.ai.application.guardrail.OutputGuard;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.semantic.EmbeddingSemanticQueryFactory;
import com.emme.assistant.ai.application.semantic.SemanticQuery;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChatServiceTest {

  @Test
  void exposesOnlyOneSpringAutowiredConstructor() {
    assertThat(
            java.util.Arrays.stream(ChatService.class.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class)))
        .hasSize(1);
  }

  private static final String INFORMATIONAL_MESSAGE = "What are your hours?";

  private static SemanticQuery informationalQuery() {
    return new SemanticQuery(
        INFORMATIONAL_MESSAGE,
        com.emme.assistant.ai.EmbeddingTestVectors.testEmbedding("embedding-v1", 1.0f, 0.0f));
  }

  private static EmbeddingService embeddingsFor(SemanticQuery query) {
    EmbeddingService embeddings = mock(EmbeddingService.class);
    when(embeddings.embed(query.text())).thenReturn(query.embedding());
    return embeddings;
  }

  @Test
  void preparesOneEmbeddingWhenToolRoutingAndSemanticCacheShareTheTurn() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    EmbeddingService embeddings = mock(EmbeddingService.class);
    ProactiveToolRouter router = mock(ProactiveToolRouter.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    SemanticQuery query =
        new SemanticQuery(
            "What are your hours?",
            com.emme.assistant.ai.EmbeddingTestVectors.testEmbedding("embedding-v1", 1.0f, 0.0f));
    when(embeddings.embed("What are your hours?")).thenReturn(query.embedding());
    when(router.route(query)).thenReturn(Optional.empty());
    when(cache.lookup("", query)).thenReturn(Optional.empty());
    when(model.complete("", "What are your hours?")).thenReturn("Open today.");
    ChatService service =
        new ChatService(
            model,
            Optional.of(cache),
            Optional.of(router),
            Optional.of(new EmbeddingSemanticQueryFactory(embeddings)),
            mock(SemanticMetrics.class));

    assertThat(inContext(() -> service.chat("", "What are your hours?"))).isEqualTo("Open today.");

    verify(embeddings).embed("What are your hours?");
    verifyNoMoreInteractions(embeddings);
    verify(router).route(query);
    verify(cache).lookup("", query);
  }

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
    SemanticQuery query = informationalQuery();
    when(cache.lookup("", query)).thenReturn(Optional.of("Open today."));
    ChatService service =
        new ChatService(
            model,
            Optional.of(cache),
            Optional.empty(),
            Optional.of(new EmbeddingSemanticQueryFactory(embeddingsFor(query))),
            mock(SemanticMetrics.class));

    assertThat(inContext(() -> service.chat("", INFORMATIONAL_MESSAGE))).isEqualTo("Open today.");

    verifyNoInteractions(model);
  }

  @Test
  void storesAProviderResponseAfterASemanticCacheMiss() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    SemanticQuery query = informationalQuery();
    when(cache.lookup("", query)).thenReturn(Optional.empty());
    when(model.complete("", INFORMATIONAL_MESSAGE)).thenReturn("Open today.");
    ChatService service =
        new ChatService(
            model,
            Optional.of(cache),
            Optional.empty(),
            Optional.of(new EmbeddingSemanticQueryFactory(embeddingsFor(query))),
            mock(SemanticMetrics.class));

    assertThat(inContext(() -> service.chat("", INFORMATIONAL_MESSAGE))).isEqualTo("Open today.");

    verify(cache).store("", query, "Open today.");
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
    SemanticQuery query = informationalQuery();
    when(cache.lookup("", query)).thenThrow(new IllegalStateException("database unavailable"));
    when(model.complete("", INFORMATIONAL_MESSAGE)).thenReturn("Open today.");
    ChatService service =
        new ChatService(
            model,
            Optional.of(cache),
            Optional.empty(),
            Optional.of(new EmbeddingSemanticQueryFactory(embeddingsFor(query))),
            mock(SemanticMetrics.class));

    assertThat(inContext(() -> service.chat("", INFORMATIONAL_MESSAGE))).isEqualTo("Open today.");
    verify(model).complete("", INFORMATIONAL_MESSAGE);
  }

  @Test
  void recordsTheReasonWhenSemanticCacheFailureFallsBackToTheModel() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    SemanticMetrics metrics = mock(SemanticMetrics.class);
    SemanticQuery query = informationalQuery();
    when(cache.lookup("", query)).thenThrow(new IllegalStateException("database unavailable"));
    when(model.complete("", INFORMATIONAL_MESSAGE)).thenReturn("Open today.");
    ChatService service =
        new ChatService(
            model,
            Optional.of(cache),
            Optional.empty(),
            Optional.of(new EmbeddingSemanticQueryFactory(embeddingsFor(query))),
            metrics);

    assertThat(inContext(() -> service.chat("", INFORMATIONAL_MESSAGE))).isEqualTo("Open today.");

    verify(metrics).recordFallback("chat", "semantic_cache_failure");
  }

  @Test
  void propagatesSecurityFailuresFromSemanticCacheInsteadOfFallingBack() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    SecurityException failure = new SecurityException("tenant access denied");
    SemanticQuery query = informationalQuery();
    when(cache.lookup("", query)).thenThrow(failure);
    ChatService service =
        new ChatService(
            model,
            Optional.of(cache),
            Optional.empty(),
            Optional.of(new EmbeddingSemanticQueryFactory(embeddingsFor(query))),
            mock(SemanticMetrics.class));

    assertThatThrownBy(() -> inContext(() -> service.chat("", INFORMATIONAL_MESSAGE)))
        .isSameAs(failure);
    verifyNoInteractions(model);
  }

  @Test
  void keepsTheModelResponseWhenSemanticCacheWriteInfrastructureFails() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    SemanticQuery query = informationalQuery();
    when(cache.lookup("", query)).thenReturn(Optional.empty());
    when(model.complete("", INFORMATIONAL_MESSAGE)).thenReturn("Open today.");
    when(cache.store("", query, "Open today."))
        .thenThrow(new IllegalStateException("database unavailable"));
    ChatService service =
        new ChatService(
            model,
            Optional.of(cache),
            Optional.empty(),
            Optional.of(new EmbeddingSemanticQueryFactory(embeddingsFor(query))),
            mock(SemanticMetrics.class));

    assertThat(inContext(() -> service.chat("", INFORMATIONAL_MESSAGE))).isEqualTo("Open today.");
  }

  @Test
  void returnsAProactiveToolResultBeforeCacheOrModelExecution() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    EmbeddingService embeddings = mock(EmbeddingService.class);
    SemanticResponseCache cache = mock(SemanticResponseCache.class);
    ProactiveToolRouter router = mock(ProactiveToolRouter.class);
    SemanticQuery query =
        new SemanticQuery(
            "what services do you have?",
            com.emme.assistant.ai.EmbeddingTestVectors.testEmbedding("embedding-v1", 1.0f, 0.0f));
    when(embeddings.embed("what services do you have?")).thenReturn(query.embedding());
    when(router.route(query))
        .thenReturn(Optional.of(new AiToolResult("getSalonServices", "services", true)));
    ChatService service =
        new ChatService(
            model,
            Optional.of(cache),
            Optional.of(router),
            Optional.of(new EmbeddingSemanticQueryFactory(embeddings)),
            mock(SemanticMetrics.class));

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

  @Test
  void rejectsInputBeforeSemanticOrModelExecutionWhenTheGuardBlocksIt() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    InputGuard inputGuard = mock(InputGuard.class);
    when(inputGuard.check(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            new com.emme.ai.contracts.guardrail.GuardrailDecision(
                com.emme.ai.contracts.guardrail.GuardrailAction.BLOCK,
                "input.blocked",
                java.util.Map.of()));
    ChatService service =
        new ChatService(
            model,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            mock(SemanticMetrics.class),
            Optional.of(inputGuard),
            Optional.<OutputGuard>empty());

    assertThatThrownBy(() -> inContext(() -> service.chat("", "hello")))
        .isInstanceOf(GuardrailRejectedException.class)
        .hasMessage("AI input rejected by guardrail: input.blocked");
    verifyNoInteractions(model);
  }

  @Test
  void preservesBlankMessageCompatibilityWithoutInvokingTheDirectInputGuard() {
    ChatCompletionPort model = mock(ChatCompletionPort.class);
    InputGuard inputGuard = mock(InputGuard.class);
    when(model.complete("", "")).thenReturn("response");
    ChatService service =
        new ChatService(
            model,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            mock(SemanticMetrics.class),
            Optional.of(inputGuard),
            Optional.<OutputGuard>empty());

    assertThat(inContext(() -> service.chat("", ""))).isEqualTo("response");

    verifyNoInteractions(inputGuard);
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
