package com.emme.ai.contracts.rag;

import java.util.Map;
import java.util.Objects;

/** Tenant-filtered reference document returned by a knowledge retriever. */
public record RetrievedDocument(
    String sourceId, String content, Map<String, String> metadata, double score) {

  public RetrievedDocument {
    sourceId = requireText(sourceId, "sourceId");
    content = requireText(content, "content");
    Objects.requireNonNull(metadata, "metadata must not be null");
    metadata = Map.copyOf(metadata);
    if (!Double.isFinite(score) || score < 0) {
      throw new IllegalArgumentException("score must be finite and non-negative");
    }
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
