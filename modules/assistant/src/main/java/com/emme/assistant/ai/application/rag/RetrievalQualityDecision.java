package com.emme.assistant.ai.application.rag;

import java.util.Objects;

/** Immutable, text-free explanation of a retrieval-quality decision. */
public record RetrievalQualityDecision(
    boolean accepted,
    double topScore,
    double secondScore,
    double margin,
    int supportingDocumentCount,
    int freshDocumentCount,
    boolean lexicalAgreement,
    String reasonCode) {

  public RetrievalQualityDecision {
    if (!Double.isFinite(topScore) || topScore < 0.0) {
      throw new IllegalArgumentException("topScore must be finite and non-negative");
    }
    if (!Double.isFinite(secondScore) || secondScore < 0.0) {
      throw new IllegalArgumentException("secondScore must be finite and non-negative");
    }
    if (!Double.isFinite(margin) || margin < 0.0) {
      throw new IllegalArgumentException("margin must be finite and non-negative");
    }
    if (supportingDocumentCount < 0 || freshDocumentCount < 0) {
      throw new IllegalArgumentException("document counts must be non-negative");
    }
    Objects.requireNonNull(reasonCode, "reasonCode must not be null");
    if (reasonCode.isBlank() || reasonCode.length() > 64) {
      throw new IllegalArgumentException("reasonCode must contain 1 to 64 characters");
    }
  }
}
