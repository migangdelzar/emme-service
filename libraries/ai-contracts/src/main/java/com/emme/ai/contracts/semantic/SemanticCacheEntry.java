package com.emme.ai.contracts.semantic;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Durable metadata and response payload for a scoped, expiring semantic cache entry. */
public record SemanticCacheEntry(
    String id,
    UUID tenantId,
    String audienceScope,
    EmbeddingVector embedding,
    String query,
    String response,
    String promptVersion,
    String policyVersion,
    Instant expiresAt) {

  public SemanticCacheEntry {
    id = requireText(id, "id");
    tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    audienceScope = requireText(audienceScope, "audienceScope");
    embedding = Objects.requireNonNull(embedding, "embedding must not be null");
    query = requireText(query, "query");
    response = requireText(response, "response");
    promptVersion = requireText(promptVersion, "promptVersion");
    policyVersion = requireText(policyVersion, "policyVersion");
    expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    if (!expiresAt.isAfter(Instant.now())) {
      throw new IllegalArgumentException("expiresAt must be in the future");
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
