package com.emme.assistant.ai.application.port.out;

import com.emme.ai.contracts.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.semantic.SemanticCacheIdentity;
import com.emme.assistant.ai.application.semantic.SemanticCacheInvalidation;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Durable, principal-scoped semantic-cache boundary. Implementations own tenant resolution. */
public interface SemanticCachePort {

  /** Finds active, unexpired candidates for one cache context. */
  List<Candidate> find(Lookup lookup, int limit);

  /** Persists a cache response and returns the durable row id for the idempotency key. */
  UUID put(Put write);

  /** Atomically increments the durable hit counter for an active, unexpired cache row. */
  boolean recordHit(UUID cacheId);

  /** Invalidates a tenant-wide or principal-scoped cache target. */
  default void invalidate(SemanticCacheInvalidation invalidation) {
    throw new UnsupportedOperationException("Semantic cache invalidation is not implemented");
  }

  record Lookup(
      String cacheKind,
      String contextFingerprint,
      String promptVersion,
      EmbeddingVector query,
      SemanticCacheIdentity identity) {
    public Lookup {
      requireText(cacheKind, "cacheKind");
      requireText(contextFingerprint, "contextFingerprint");
      requireText(promptVersion, "promptVersion");
      if (query == null) {
        throw new NullPointerException("query must not be null");
      }
      Objects.requireNonNull(identity, "identity must not be null");
    }
  }

  record Candidate(UUID id, String responsePayload, double similarity) {

    public Candidate {
      if (id == null) {
        throw new NullPointerException("id must not be null");
      }
      requireText(responsePayload, "responsePayload");
      if (!Double.isFinite(similarity) || similarity < -1.0 || similarity > 1.0) {
        throw new IllegalArgumentException("similarity must be between -1 and 1");
      }
    }
  }

  record Put(
      String cacheKind,
      String queryText,
      String contextFingerprint,
      String promptVersion,
      String responsePayload,
      Instant expiresAt,
      EmbeddingVector query,
      String writeIdempotencyKey,
      SemanticCacheIdentity identity) {
    public Put {
      requireText(cacheKind, "cacheKind");
      requireText(queryText, "queryText");
      requireText(contextFingerprint, "contextFingerprint");
      requireText(promptVersion, "promptVersion");
      requireText(responsePayload, "responsePayload");
      Objects.requireNonNull(expiresAt, "expiresAt must not be null");
      Objects.requireNonNull(query, "query must not be null");
      requireText(writeIdempotencyKey, "writeIdempotencyKey");
      Objects.requireNonNull(identity, "identity must not be null");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
