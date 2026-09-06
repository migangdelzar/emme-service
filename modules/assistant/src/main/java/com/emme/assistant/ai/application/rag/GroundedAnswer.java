package com.emme.assistant.ai.application.rag;

import java.util.List;
import java.util.Objects;

/** Answer result carrying only bounded retrieval evidence, never document text. */
public record GroundedAnswer(
    String text,
    KnowledgeRoute route,
    RetrievalQualityDecision retrieval,
    boolean grounded,
    List<String> sourceIds) {

  public GroundedAnswer(
      String text, KnowledgeRoute route, RetrievalQualityDecision retrieval, boolean grounded) {
    this(text, route, retrieval, grounded, List.of());
  }

  public GroundedAnswer {
    Objects.requireNonNull(text, "text must not be null");
    if (text.isBlank()) {
      throw new IllegalArgumentException("text must not be blank");
    }
    Objects.requireNonNull(route, "route must not be null");
    Objects.requireNonNull(retrieval, "retrieval must not be null");
    Objects.requireNonNull(sourceIds, "sourceIds must not be null");
    sourceIds = List.copyOf(sourceIds);
    if (sourceIds.stream().anyMatch(sourceId -> sourceId == null || sourceId.isBlank())) {
      throw new IllegalArgumentException("sourceIds must not contain blank values");
    }
    if (grounded && !retrieval.accepted()) {
      throw new IllegalArgumentException("grounded answers require an accepted retrieval");
    }
  }
}
