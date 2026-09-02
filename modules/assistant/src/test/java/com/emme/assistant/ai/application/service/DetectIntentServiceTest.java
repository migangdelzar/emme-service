package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    DetectIntentService service = new DetectIntentService(Optional.empty());

    assertThatThrownBy(() -> service.detect("hello"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void usesTheSemanticRouteWhenAvailable() {
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    IntentResult semanticResult = new IntentResult("BOOK_APPOINTMENT", 0.98, Map.of());
    when(semantic.route("book it")).thenReturn(Optional.of(semanticResult));
    DetectIntentService service = new DetectIntentService(Optional.of(semantic));

    assertThat(inContext(() -> service.detect("book it"))).isEqualTo(semanticResult);

    verify(semantic).route("book it");
  }

  @Test
  void returnsAZeroConfidenceGeneralIntentWhenSemanticRoutingAbstains() {
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    when(semantic.route("unclear")).thenReturn(Optional.empty());
    DetectIntentService service = new DetectIntentService(Optional.of(semantic));

    assertThat(inContext(() -> service.detect("unclear")))
        .isEqualTo(new IntentResult("GENERAL", 0.0, Map.of("routing", "abstained")));

    verify(semantic).route("unclear");
  }

  @Test
  void returnsAZeroConfidenceGeneralIntentWhenTheEmbeddingProviderIsUnavailable() {
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    when(semantic.route("book it"))
        .thenThrow(new EmbeddingProviderUnavailableException("local unavailable"));
    DetectIntentService service = new DetectIntentService(Optional.of(semantic));

    assertThat(inContext(() -> service.detect("book it")))
        .isEqualTo(new IntentResult("GENERAL", 0.0, Map.of("routing", "unavailable")));
    verify(semantic).route("book it");
  }

  @Test
  void returnsAZeroConfidenceGeneralIntentWhenTheSemanticVectorStoreReportsATransientFailure() {
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    when(semantic.route("book it"))
        .thenThrow(new TransientDataAccessResourceException("vector store unavailable"));
    DetectIntentService service = new DetectIntentService(Optional.of(semantic));

    assertThat(inContext(() -> service.detect("book it")))
        .isEqualTo(new IntentResult("GENERAL", 0.0, Map.of("routing", "unavailable")));
    verify(semantic).route("book it");
  }

  @Test
  void propagatesNonTransientSemanticPersistenceFailures() {
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    IllegalStateException persistenceFailure = new IllegalStateException("persistence failure");
    when(semantic.route("book it")).thenThrow(persistenceFailure);
    DetectIntentService service = new DetectIntentService(Optional.of(semantic));

    assertThatThrownBy(() -> inContext(() -> service.detect("book it")))
        .isSameAs(persistenceFailure);
    verify(semantic).route("book it");
  }

  @Test
  void returnsAZeroConfidenceGeneralIntentWhenSemanticRoutingIsDisabled() {
    DetectIntentService service = new DetectIntentService(Optional.empty());

    assertThat(inContext(() -> service.detect("hello")))
        .isEqualTo(new IntentResult("GENERAL", 0.0, Map.of("routing", "unavailable")));
  }

  private static <T> T inContext(java.util.function.Supplier<T> action) {
    UUID id = UUID.randomUUID();
    return AiExecutionContextScope.call(
        new AiExecutionContext(
            UUID.randomUUID(), UUID.randomUUID(), Set.of("ROLE_CLIENT"), id, id, "trace", "id"),
        action::get);
  }
}
