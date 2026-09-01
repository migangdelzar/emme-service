package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.api.result.IntentResult;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.semantic.SemanticIntentRouter;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;

class DetectIntentServiceTest {

  @Test
  void rejectsIntentDetectionWithoutBackendAiExecutionContext() {
    AiModelProvider fallback = mock(AiModelProvider.class);
    DetectIntentService service = new DetectIntentService(fallback, Optional.empty());

    assertThatThrownBy(() -> service.detect("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void usesTheSemanticRouteBeforeTheModelFallback() {
    AiModelProvider fallback = mock(AiModelProvider.class);
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    IntentResult semanticResult = new IntentResult("BOOK_APPOINTMENT", 0.98, Map.of());
    when(semantic.route("book it")).thenReturn(Optional.of(semanticResult));
    DetectIntentService service = new DetectIntentService(fallback, Optional.of(semantic));

    assertThat(inContext(() -> service.detect("book it"))).isEqualTo(semanticResult);

    verifyNoInteractions(fallback);
  }

  @Test
  void fallsBackToTheModelWhenSemanticRoutingAbstains() {
    AiModelProvider fallback = mock(AiModelProvider.class);
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    when(semantic.route("unclear")).thenReturn(Optional.empty());
    when(fallback.routeIntent("unclear"))
        .thenReturn(new AiModelProvider.IntentResult("GENERAL", 0.70, Map.of()));
    DetectIntentService service = new DetectIntentService(fallback, Optional.of(semantic));

    assertThat(inContext(() -> service.detect("unclear")))
        .isEqualTo(new IntentResult("GENERAL", 0.70, Map.of()));

    verify(fallback).routeIntent("unclear");
  }

  @Test
  void fallsBackToTheModelOnlyWhenTheEmbeddingProviderIsUnavailable() {
    AiModelProvider fallback = mock(AiModelProvider.class);
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    when(semantic.route("book it"))
        .thenThrow(new EmbeddingProviderUnavailableException("local unavailable"));
    when(fallback.routeIntent("book it"))
        .thenReturn(new AiModelProvider.IntentResult("BOOK", 0.95, Map.of()));
    DetectIntentService service = new DetectIntentService(fallback, Optional.of(semantic));

    assertThat(inContext(() -> service.detect("book it")).intent()).isEqualTo("BOOK");
    verify(fallback).routeIntent("book it");
  }

  @Test
  void fallsBackWhenTheSemanticVectorStoreReportsATransientFailure() {
    AiModelProvider fallback = mock(AiModelProvider.class);
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    when(semantic.route("book it"))
        .thenThrow(new TransientDataAccessResourceException("vector store unavailable"));
    when(fallback.routeIntent("book it"))
        .thenReturn(new AiModelProvider.IntentResult("BOOK", 0.95, Map.of()));
    DetectIntentService service = new DetectIntentService(fallback, Optional.of(semantic));

    assertThat(inContext(() -> service.detect("book it")).intent()).isEqualTo("BOOK");
    verify(fallback).routeIntent("book it");
  }

  @Test
  void propagatesNonTransientSemanticPersistenceFailuresInsteadOfUsingTheModelFallback() {
    AiModelProvider fallback = mock(AiModelProvider.class);
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    IllegalStateException persistenceFailure = new IllegalStateException("persistence failure");
    when(semantic.route("book it")).thenThrow(persistenceFailure);
    DetectIntentService service = new DetectIntentService(fallback, Optional.of(semantic));

    assertThatThrownBy(() -> inContext(() -> service.detect("book it")))
        .isSameAs(persistenceFailure);
    verifyNoInteractions(fallback);
  }

  private static <T> T inContext(java.util.function.Supplier<T> action) {
    UUID id = UUID.randomUUID();
    return AiExecutionContextScope.call(
        new AiExecutionContext(
            UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_CLIENT"), id, id, "trace", "id"),
        action::get);
  }
}
