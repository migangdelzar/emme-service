package com.emme.assistant.ai.domain.quote;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Deterministic quote candidate and the safety decision attached to it. */
public record QuoteCalculation(
    String templateKey,
    String templateVersion,
    String currency,
    List<QuoteLine> appliedLines,
    BigDecimal minimumPrice,
    BigDecimal maximumPrice,
    int durationMinutes,
    boolean needsHumanReview,
    List<String> reviewReasons) {

  public QuoteCalculation {
    Objects.requireNonNull(templateKey, "templateKey must not be null");
    Objects.requireNonNull(templateVersion, "templateVersion must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
    appliedLines =
        List.copyOf(Objects.requireNonNull(appliedLines, "appliedLines must not be null"));
    Objects.requireNonNull(minimumPrice, "minimumPrice must not be null");
    Objects.requireNonNull(maximumPrice, "maximumPrice must not be null");
    reviewReasons =
        List.copyOf(Objects.requireNonNull(reviewReasons, "reviewReasons must not be null"));
  }
}
