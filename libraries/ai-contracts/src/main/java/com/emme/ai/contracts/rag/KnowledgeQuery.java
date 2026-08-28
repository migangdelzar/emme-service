package com.emme.ai.contracts.rag;

import java.util.Objects;

/** Tenant-safe unstructured knowledge query; authoritative transactional data is not RAG input. */
public record KnowledgeQuery(String text, String locale, int limit) {

  public KnowledgeQuery {
    text = requireText(text, "text");
    locale = requireText(locale, "locale");
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
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
