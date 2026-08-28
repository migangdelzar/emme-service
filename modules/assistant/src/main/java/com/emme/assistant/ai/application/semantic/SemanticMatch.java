package com.emme.assistant.ai.application.semantic;

/** A ranked semantic reference and its cosine similarity. */
public record SemanticMatch(String key, double similarity) {

  public SemanticMatch {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Semantic match key must not be blank");
    }
    if (!Double.isFinite(similarity)) {
      throw new IllegalArgumentException("Semantic similarity must be finite");
    }
  }
}
