package com.emme.documents.api.result;

import java.util.UUID;

/** Stable public representation of a document chunk. */
public record DocumentChunkDetails(
    UUID id,
    UUID documentId,
    int chunkIndex,
    String content,
    String contentFingerprint,
    double score) {

  public DocumentChunkDetails(
      UUID id, UUID documentId, int chunkIndex, String content, String contentFingerprint) {
    this(id, documentId, chunkIndex, content, contentFingerprint, 0.0);
  }

  public DocumentChunkDetails {
    if (!Double.isFinite(score) || score < 0.0) {
      throw new IllegalArgumentException("score must be finite and non-negative");
    }
  }
}
