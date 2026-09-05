package com.emme.assistant.ai.application.rag;

import java.time.Duration;
import java.util.Objects;

/** Route-specific acceptance thresholds for grounded retrieval context. */
public record RetrievalQualityPolicy(
    double minimumTopScore,
    double minimumMargin,
    int minimumSupportingDocuments,
    Duration maximumDocumentAge,
    boolean requireLexicalAgreement) {

  public RetrievalQualityPolicy {
    if (!Double.isFinite(minimumTopScore) || minimumTopScore < 0.0 || minimumTopScore > 1.0) {
      throw new IllegalArgumentException("minimumTopScore must be between 0 and 1");
    }
    if (!Double.isFinite(minimumMargin) || minimumMargin < 0.0 || minimumMargin > 1.0) {
      throw new IllegalArgumentException("minimumMargin must be between 0 and 1");
    }
    if (minimumSupportingDocuments < 1) {
      throw new IllegalArgumentException("minimumSupportingDocuments must be positive");
    }
    maximumDocumentAge =
        Objects.requireNonNull(maximumDocumentAge, "maximumDocumentAge must not be null");
    if (maximumDocumentAge.isZero() || maximumDocumentAge.isNegative()) {
      throw new IllegalArgumentException("maximumDocumentAge must be positive");
    }
  }
}
