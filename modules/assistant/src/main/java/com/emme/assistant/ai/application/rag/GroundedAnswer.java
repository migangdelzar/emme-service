package com.emme.assistant.ai.application.rag;

import java.util.Objects;

/** Answer result carrying only bounded retrieval evidence, never document text. */
public record GroundedAnswer(
    String text, KnowledgeRoute route, RetrievalQualityDecision retrieval, boolean grounded) {

  public GroundedAnswer {
    Objects.requireNonNull(text, "text must not be null");
    if (text.isBlank()) {
      throw new IllegalArgumentException("text must not be blank");
    }
    Objects.requireNonNull(route, "route must not be null");
    Objects.requireNonNull(retrieval, "retrieval must not be null");
    if (grounded && !retrieval.accepted()) {
      throw new IllegalArgumentException("grounded answers require an accepted retrieval");
    }
  }
}
