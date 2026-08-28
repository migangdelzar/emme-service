package com.emme.assistant.ai.domain.quote;

import java.util.List;
import java.util.Objects;

/** Immutable tenant-specific and versioned quote template. */
public record QuoteTemplate(
    String key, String version, String currency, List<QuoteTemplateLine> lines) {

  public QuoteTemplate {
    requireText(key, "key");
    requireText(version, "version");
    requireText(currency, "currency");
    if (currency.length() != 3) {
      throw new IllegalArgumentException("currency must be an ISO 4217 code");
    }
    Objects.requireNonNull(lines, "lines must not be null");
    if (lines.isEmpty()) {
      throw new IllegalArgumentException("Quote template must define at least one line");
    }
    if (lines.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("lines must not contain null values");
    }
    lines = List.copyOf(lines);
    if (lines.stream().noneMatch(line -> line.type() == QuoteLineType.REQUIRED_SERVICE)) {
      throw new IllegalArgumentException(
          "Quote template must define at least one required service line");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
