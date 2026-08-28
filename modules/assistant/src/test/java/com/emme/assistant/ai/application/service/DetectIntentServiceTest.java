package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.api.result.IntentResult;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.semantic.SemanticIntentRouter;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DetectIntentServiceTest {

  @Test
  void usesTheSemanticRouteBeforeTheModelFallback() {
    AiModelProvider fallback = mock(AiModelProvider.class);
    SemanticIntentRouter semantic = mock(SemanticIntentRouter.class);
    IntentResult semanticResult = new IntentResult("BOOK_APPOINTMENT", 0.98, Map.of());
    when(semantic.route("book it")).thenReturn(Optional.of(semanticResult));
    DetectIntentService service = new DetectIntentService(fallback, Optional.of(semantic));

    assertThat(service.detect("book it")).isEqualTo(semanticResult);

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

    assertThat(service.detect("unclear")).isEqualTo(new IntentResult("GENERAL", 0.70, Map.of()));

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

    assertThat(service.detect("book it").intent()).isEqualTo("BOOK");
    verify(fallback).routeIntent("book it");
  }
}
