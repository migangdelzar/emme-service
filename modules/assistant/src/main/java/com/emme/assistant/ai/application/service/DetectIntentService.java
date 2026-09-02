package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.result.IntentResult;
import com.emme.assistant.ai.api.usecase.DetectIntentUseCase;
import com.emme.assistant.ai.application.semantic.SemanticFailurePolicy;
import com.emme.assistant.ai.application.semantic.SemanticIntentRouter;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DetectIntentService implements DetectIntentUseCase {
  private final Optional<SemanticIntentRouter> semanticRouter;

  public DetectIntentService(Optional<SemanticIntentRouter> semanticRouter) {
    this.semanticRouter =
        java.util.Objects.requireNonNull(semanticRouter, "semanticRouter must not be null");
  }

  @Override
  public IntentResult detect(String message) {
    AiExecutionContextScope.requireCurrent();
    if (semanticRouter.isEmpty()) {
      return safeResult("unavailable");
    }
    try {
      return semanticRouter
          .flatMap(router -> router.route(message))
          .orElseGet(() -> safeResult("abstained"));
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      if (!SemanticFailurePolicy.isTransientVectorOrProviderFailure(failure)) {
        throw failure;
      }
      return safeResult("unavailable");
    }
  }

  private static IntentResult safeResult(String routingStatus) {
    return new IntentResult("GENERAL", 0.0, Map.of("routing", routingStatus));
  }
}
