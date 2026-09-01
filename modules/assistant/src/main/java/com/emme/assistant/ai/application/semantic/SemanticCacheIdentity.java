package com.emme.assistant.ai.application.semantic;

import java.util.Objects;

/** Versioned identity of the response and knowledge used to produce a cacheable answer. */
public record SemanticCacheIdentity(
    String responseProvider,
    String responseModel,
    String knowledgeVersion,
    String policyVersion,
    String sourceVersion) {

  public SemanticCacheIdentity {
    responseProvider = requireText(responseProvider, "responseProvider");
    responseModel = requireText(responseModel, "responseModel");
    knowledgeVersion = requireText(knowledgeVersion, "knowledgeVersion");
    policyVersion = requireText(policyVersion, "policyVersion");
    sourceVersion = requireText(sourceVersion, "sourceVersion");
  }

  public static SemanticCacheIdentity legacy() {
    return new SemanticCacheIdentity(
        "legacy-provider", "legacy-model", "legacy-knowledge", "legacy-policy", "legacy-source");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
