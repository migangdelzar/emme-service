package com.emme.assistant.ai.domain.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeterministicQuoteCalculatorTest {

  private static final NailDesignFeatures HIGH_CONFIDENCE_FEATURES =
      new NailDesignFeatures(
          NailShape.ALMOND,
          NailLength.MEDIUM,
          "milky pink",
          List.of(NailEffect.CHROME),
          List.of(),
          ExtensionType.NONE,
          false,
          false,
          ArtComplexity.MODERATE,
          Map.of("shape", 0.99, "artComplexity", 0.95),
          List.of(),
          false);

  @Test
  void sumsOnlyApplicableTenantTemplateLinesWithoutUsingTheModelForPricing() {
    QuoteTemplate template =
        new QuoteTemplate(
            "classic-manicure",
            "2026-08-01",
            "MXN",
            List.of(
                line(
                    "base",
                    QuoteLineType.REQUIRED_SERVICE,
                    "300.00",
                    "350.00",
                    60,
                    QuoteFeatureCondition.always()),
                line(
                    "chrome",
                    QuoteLineType.ADD_ON,
                    "80.00",
                    "100.00",
                    15,
                    QuoteFeatureCondition.forEffect(NailEffect.CHROME)),
                line(
                    "ombre",
                    QuoteLineType.ADD_ON,
                    "100.00",
                    "150.00",
                    20,
                    QuoteFeatureCondition.forEffect(NailEffect.OMBRE))));

    QuoteCalculation calculation =
        new DeterministicQuoteCalculator(0.80).calculate(HIGH_CONFIDENCE_FEATURES, template);

    assertThat(calculation.minimumPrice()).isEqualByComparingTo("380.00");
    assertThat(calculation.maximumPrice()).isEqualByComparingTo("450.00");
    assertThat(calculation.durationMinutes()).isEqualTo(75);
    assertThat(calculation.appliedLines())
        .extracting(QuoteLine::code)
        .containsExactly("base", "chrome");
    assertThat(calculation.needsHumanReview()).isFalse();
  }

  @Test
  void pausesForReviewWhenAConditionalPriceCannotBeEvaluatedSafely() {
    NailDesignFeatures unknownExtension =
        new NailDesignFeatures(
            NailShape.ALMOND,
            NailLength.LONG,
            null,
            List.of(),
            List.of(),
            null,
            null,
            null,
            ArtComplexity.MODERATE,
            Map.of(),
            List.of(),
            false);
    QuoteTemplate template =
        new QuoteTemplate(
            "extensions",
            "v3",
            "MXN",
            List.of(
                line(
                    "base",
                    QuoteLineType.REQUIRED_SERVICE,
                    "400.00",
                    "450.00",
                    75,
                    QuoteFeatureCondition.always()),
                line(
                    "gel-extension",
                    QuoteLineType.ADD_ON,
                    "250.00",
                    "350.00",
                    60,
                    QuoteFeatureCondition.forExtension(ExtensionType.GEL))));

    QuoteCalculation calculation =
        new DeterministicQuoteCalculator(0.80).calculate(unknownExtension, template);

    assertThat(calculation.needsHumanReview()).isTrue();
    assertThat(calculation.reviewReasons())
        .contains("extensionType is required to price 'gel-extension'");
    assertThat(calculation.minimumPrice()).isEqualByComparingTo("400.00");
  }

  @Test
  void routesLowConfidenceFeaturesToReviewEvenWhenAFormulaCanProduceACandidate() {
    NailDesignFeatures lowConfidence =
        new NailDesignFeatures(
            NailShape.ALMOND,
            NailLength.MEDIUM,
            "pink",
            List.of(),
            List.of(),
            ExtensionType.NONE,
            false,
            false,
            ArtComplexity.COMPLEX,
            Map.of("artComplexity", 0.40),
            List.of(),
            false);

    QuoteCalculation calculation =
        new DeterministicQuoteCalculator(0.80)
            .calculate(
                lowConfidence,
                new QuoteTemplate(
                    "base",
                    "v1",
                    "MXN",
                    List.of(
                        line(
                            "base",
                            QuoteLineType.REQUIRED_SERVICE,
                            "300.00",
                            "350.00",
                            60,
                            QuoteFeatureCondition.always()))));

    assertThat(calculation.needsHumanReview()).isTrue();
    assertThat(calculation.reviewReasons()).contains("artComplexity confidence is below 0.8");
  }

  @Test
  void rejectsTemplatesWithoutARequiredServiceLine() {
    assertThatThrownBy(
            () ->
                new QuoteTemplate(
                    "invalid",
                    "v1",
                    "MXN",
                    List.of(
                        line(
                            "chrome",
                            QuoteLineType.ADD_ON,
                            "80.00",
                            "100.00",
                            15,
                            QuoteFeatureCondition.always()))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Quote template must define at least one required service line");
  }

  private static QuoteTemplateLine line(
      String code,
      QuoteLineType type,
      String minimum,
      String maximum,
      int duration,
      QuoteFeatureCondition condition) {
    return new QuoteTemplateLine(
        code, type, new BigDecimal(minimum), new BigDecimal(maximum), duration, condition);
  }
}
