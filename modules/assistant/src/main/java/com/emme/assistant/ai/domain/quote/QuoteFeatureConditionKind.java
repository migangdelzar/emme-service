package com.emme.assistant.ai.domain.quote;

/** Supported deterministic predicates for tenant quote-template lines. */
public enum QuoteFeatureConditionKind {
  ALWAYS,
  EFFECT,
  DECORATION,
  EXTENSION_TYPE,
  REMOVAL_REQUIRED,
  REPAIR_REQUIRED,
  ART_COMPLEXITY
}
