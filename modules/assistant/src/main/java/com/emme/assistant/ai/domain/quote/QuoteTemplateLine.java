package com.emme.assistant.ai.domain.quote;

import java.math.BigDecimal;
import java.util.Objects;

/** Tenant-owned versioned pricing line evaluated by deterministic rules. */
public record QuoteTemplateLine(
    String code,
    QuoteLineType type,
    BigDecimal minimumPrice,
    BigDecimal maximumPrice,
    int durationMinutes,
    QuoteFeatureCondition condition) {

  public QuoteTemplateLine {
    requireText(code, "code");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(minimumPrice, "minimumPrice must not be null");
    Objects.requireNonNull(maximumPrice, "maximumPrice must not be null");
    if (minimumPrice.signum() < 0) {
      throw new IllegalArgumentException("minimumPrice must not be negative");
    }
    if (maximumPrice.compareTo(minimumPrice) < 0) {
      throw new IllegalArgumentException("maximumPrice must not be less than minimumPrice");
    }
    if (durationMinutes < 1 || durationMinutes > 1440) {
      throw new IllegalArgumentException("durationMinutes must be between 1 and 1440");
    }
    Objects.requireNonNull(condition, "condition must not be null");
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
