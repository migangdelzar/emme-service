package com.emme.ai.contracts.routing;

import java.util.Objects;
import java.util.Set;

/** Normalized input and explicit route hints supplied to the intent router. */
public record RouteRequest(String message, String locale, Set<String> explicitIntents) {

  public RouteRequest {
    message = requireText(message, "message");
    locale = requireText(locale, "locale");
    explicitIntents = Objects.requireNonNull(explicitIntents, "explicitIntents must not be null");
    if (explicitIntents.stream().anyMatch(intent -> intent == null || intent.isBlank())) {
      throw new IllegalArgumentException("explicitIntents must not contain blank values");
    }
    explicitIntents = Set.copyOf(explicitIntents);
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
