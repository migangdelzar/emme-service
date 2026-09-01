package com.emme.assistant.ai.application.trace;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Durable audit record for semantic routing, cache decisions, and invalidation. */
public record AiSemanticExecutionTrace(
    UUID executionId,
    UUID tenantId,
    UUID principalId,
    String operation,
    String outcome,
    double top1Similarity,
    double top2Similarity,
    double margin,
    List<String> matches,
    String dependency,
    String dependencyVersion,
    String invalidationContext,
    long latencyMillis) {

  public AiSemanticExecutionTrace {
    Objects.requireNonNull(executionId, "executionId must not be null");
    requireText(operation, "operation");
    requireText(outcome, "outcome");
    if (!Double.isFinite(top1Similarity)
        || !Double.isFinite(top2Similarity)
        || !Double.isFinite(margin)
        || margin < 0) {
      throw new IllegalArgumentException("Semantic trace scores must be finite and non-negative");
    }
    matches = matches == null ? List.of() : List.copyOf(matches);
    if (latencyMillis < 0) {
      throw new IllegalArgumentException("latencyMillis must not be negative");
    }
    if ((dependency == null) != (dependencyVersion == null)) {
      throw new IllegalArgumentException("dependency and dependencyVersion must be paired");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
