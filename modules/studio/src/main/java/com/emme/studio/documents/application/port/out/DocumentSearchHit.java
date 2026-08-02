package com.emme.studio.documents.application.port.out;

import java.util.Objects;
import java.util.UUID;

/** Ranked result returned by a document search implementation. */
public record DocumentSearchHit(UUID chunkId, double score) {

  public DocumentSearchHit {
    Objects.requireNonNull(chunkId, "chunkId must not be null");
    if (!Double.isFinite(score) || score < 0) {
      throw new IllegalArgumentException("score must be finite and non-negative");
    }
  }
}
