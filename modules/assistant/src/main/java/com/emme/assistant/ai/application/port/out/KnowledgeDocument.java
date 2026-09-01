package com.emme.assistant.ai.application.port.out;

import java.util.Objects;

/** Framework-neutral retrieved knowledge value; transactional truth stays outside this type. */
public record KnowledgeDocument(String sourceId, String content, double score) {

  public KnowledgeDocument {
    sourceId = requireText(sourceId, "sourceId");
    content = requireText(content, "content");
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
