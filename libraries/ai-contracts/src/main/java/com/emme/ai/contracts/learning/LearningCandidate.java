package com.emme.ai.contracts.learning;

import java.util.Objects;

/** A redacted, versioned learning example awaiting evaluation and promotion. */
public record LearningCandidate(
    String candidateKey,
    LearningCandidateKind kind,
    String referenceText,
    String locale,
    String embeddingModelVersion,
    LearningCandidateEvidence evidence) {

  public LearningCandidate {
    requireText(candidateKey, "candidateKey");
    Objects.requireNonNull(kind, "kind must not be null");
    requireText(referenceText, "referenceText");
    requireText(locale, "locale");
    requireText(embeddingModelVersion, "embeddingModelVersion");
    Objects.requireNonNull(evidence, "evidence must not be null");
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
