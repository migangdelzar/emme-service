package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.api.result.IntentResult;
import com.emme.assistant.ai.api.usecase.DetectIntentUseCase;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.semantic.SemanticIntentRouter;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DetectIntentService implements DetectIntentUseCase {
  private final AiModelProvider provider;
  private final Optional<SemanticIntentRouter> semanticRouter;

  public DetectIntentService(
      AiModelProvider provider, Optional<SemanticIntentRouter> semanticRouter) {
    this.provider = java.util.Objects.requireNonNull(provider, "provider must not be null");
    this.semanticRouter =
        java.util.Objects.requireNonNull(semanticRouter, "semanticRouter must not be null");
  }

  @Override
  public IntentResult detect(String message) {
    try {
      Optional<IntentResult> semanticResult =
          semanticRouter.flatMap(router -> router.route(message));
      if (semanticResult.isPresent()) {
        return semanticResult.orElseThrow();
      }
    } catch (EmbeddingProviderUnavailableException ignored) {
      // The configured model provider is the explicit fallback for embedding outages.
    }
    AiModelProvider.IntentResult result = provider.routeIntent(message);
    return new IntentResult(result.intent(), result.confidence(), result.parameters());
  }
}
