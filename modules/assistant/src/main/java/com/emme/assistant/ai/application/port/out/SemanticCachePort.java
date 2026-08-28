package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.application.service.EmbeddingVector;
import java.util.List;
import java.util.UUID;

/** Durable, principal-scoped semantic-cache boundary. Implementations own tenant resolution. */
public interface SemanticCachePort {

  /** Finds active, unexpired candidates for one cache context. */
  List<Candidate> find(Lookup lookup, int limit);

  record Lookup(
      String cacheKind, String contextFingerprint, String promptVersion, EmbeddingVector query) {

    public Lookup {
      requireText(cacheKind, "cacheKind");
      requireText(contextFingerprint, "contextFingerprint");
      requireText(promptVersion, "promptVersion");
      if (query == null) {
        throw new NullPointerException("query must not be null");
      }
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

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
