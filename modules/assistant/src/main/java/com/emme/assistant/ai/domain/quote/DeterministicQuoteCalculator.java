package com.emme.assistant.ai.domain.quote;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Calculates quote ranges from tenant templates without model-supplied prices. */
public final class DeterministicQuoteCalculator {

  private final double minimumFieldConfidence;

  public DeterministicQuoteCalculator(double minimumFieldConfidence) {
    if (!Double.isFinite(minimumFieldConfidence)
        || minimumFieldConfidence < 0
        || minimumFieldConfidence > 1) {
      throw new IllegalArgumentException("minimumFieldConfidence must be between 0 and 1");
    }
    this.minimumFieldConfidence = minimumFieldConfidence;
  }

  public QuoteCalculation calculate(NailDesignFeatures features, QuoteTemplate template) {
    Objects.requireNonNull(features, "features must not be null");
    Objects.requireNonNull(template, "template must not be null");

    BigDecimal minimumPrice = BigDecimal.ZERO;
    BigDecimal maximumPrice = BigDecimal.ZERO;
    int durationMinutes = 0;
    List<QuoteLine> appliedLines = new ArrayList<>();
    Set<String> reviewReasons = new LinkedHashSet<>();

    for (QuoteTemplateLine line : template.lines()) {
      if (!line.condition().isKnown(features)) {
        reviewReasons.add(
            line.condition().requiredField() + " is required to price '" + line.code() + "'");
        continue;
      }
      if (!line.condition().matches(features)) {
        continue;
      }
      appliedLines.add(
          new QuoteLine(
              line.code(),
              line.type(),
              line.minimumPrice(),
              line.maximumPrice(),
              line.durationMinutes()));
      minimumPrice = minimumPrice.add(line.minimumPrice());
      maximumPrice = maximumPrice.add(line.maximumPrice());
      durationMinutes += line.durationMinutes();
    }

    features.confidenceByField().entrySet().stream()
        .filter(entry -> entry.getValue() < minimumFieldConfidence)
        .forEach(
            entry ->
                reviewReasons.add(
                    entry.getKey() + " confidence is below " + minimumFieldConfidence));
    if (features.needsHumanReview()) {
      reviewReasons.add("model flagged the extraction for human review");
    }
    features.ambiguities().forEach(reason -> reviewReasons.add("ambiguous: " + reason));

    return new QuoteCalculation(
        template.key(),
        template.version(),
        template.currency(),
        appliedLines,
        minimumPrice,
        maximumPrice,
        durationMinutes,
        !reviewReasons.isEmpty(),
        List.copyOf(reviewReasons));
  }
}
