package com.emme.assistant.ai.application.semantic;

import com.emme.ai.contracts.semantic.EmbeddingVector;
import java.util.Objects;

/** Prepared semantic input shared by all shortcuts in one chat operation. */
public record SemanticQuery(String text, EmbeddingVector embedding) {

  public SemanticQuery {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("text must not be blank");
    }
    Objects.requireNonNull(embedding, "embedding must not be null");
  }
}
