package com.emme.ai.contracts.guardrail;

import java.util.List;
import java.util.Objects;

/** Retrieval provenance facts presented to the grounding guard. */
public record GroundingRequest(
    boolean retrievalAccepted, double topScore, double margin, List<String> sourceIds) {

  public GroundingRequest {
    requireFinite(topScore, "topScore");
    requireFinite(margin, "margin");
    if (margin < 0) throw new IllegalArgumentException("margin must not be negative");
    Objects.requireNonNull(sourceIds, "sourceIds must not be null");
    if (sourceIds.stream().anyMatch(source -> source == null || source.isBlank())) {
      throw new IllegalArgumentException("sourceIds must not contain blank values");
    }
    sourceIds = List.copyOf(sourceIds);
  }

  private static void requireFinite(double value, String field) {
    if (!Double.isFinite(value)) throw new IllegalArgumentException(field + " must be finite");
  }
}
