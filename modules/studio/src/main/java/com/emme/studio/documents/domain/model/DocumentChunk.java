package com.emme.studio.documents.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Immutable document chunk used by persistence and search adapters. */
public record DocumentChunk(
    UUID id,
    UUID tenantId,
    UUID documentId,
    int chunkIndex,
    String content,
    String contentFingerprint) {

  public DocumentChunk {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(documentId, "documentId must not be null");
    Objects.requireNonNull(content, "content must not be null");
    Objects.requireNonNull(contentFingerprint, "contentFingerprint must not be null");
    if (chunkIndex < 0) {
      throw new IllegalArgumentException("chunkIndex must not be negative");
    }
  }

  public DocumentChunk(
      UUID tenantId, UUID documentId, int chunkIndex, String content, String contentFingerprint) {
    this(UUID.randomUUID(), tenantId, documentId, chunkIndex, content, contentFingerprint);
  }
}
