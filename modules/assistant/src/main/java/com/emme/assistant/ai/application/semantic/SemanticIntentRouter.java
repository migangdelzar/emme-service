package com.emme.assistant.ai.application.semantic;

import com.emme.assistant.ai.api.result.IntentResult;
import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Runs the deterministic vector intent route before any model-based fallback. */
public final class SemanticIntentRouter {

  private final EmbeddingModelPort embeddings;
  private final SemanticIntentClassifier classifier;
  private final String locale;

  public SemanticIntentRouter(
      EmbeddingModelPort embeddings, SemanticIntentClassifier classifier, String locale) {
    this.embeddings = Objects.requireNonNull(embeddings, "embeddings must not be null");
    this.classifier = Objects.requireNonNull(classifier, "classifier must not be null");
    if (locale == null || locale.isBlank()) {
      throw new IllegalArgumentException("locale must not be blank");
    }
    this.locale = locale;
  }

  public Optional<IntentResult> route(String message) {
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
    SemanticDecision decision = classifier.classify(locale, embeddings.embed(message));
    return decision
        .selectedKey()
        .map(
            key -> new IntentResult(key, decision.top1Similarity(), Map.of("routing", "semantic")));
  }
}
