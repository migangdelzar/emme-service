package com.emme.assistant.ai.domain.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NailDesignFeaturesTest {

  @Test
  void preservesTheStrictExtractionContractAsImmutableValues() {
    NailDesignFeatures features =
        new NailDesignFeatures(
            NailShape.ALMOND,
            NailLength.MEDIUM,
            "milky pink",
            List.of(NailEffect.CHROME),
            List.of(NailDecoration.RHINESTONES),
            ExtensionType.GEL,
            false,
            null,
            ArtComplexity.MODERATE,
            Map.of("shape", 0.99, "extensionType", 0.82),
            List.of("repair status is not visible"),
            true);

    assertThat(features.effects()).containsExactly(NailEffect.CHROME);
    assertThat(features.decorations()).containsExactly(NailDecoration.RHINESTONES);
    assertThat(features.confidenceByField()).containsEntry("shape", 0.99);
    assertThat(features.ambiguities()).containsExactly("repair status is not visible");

    assertThatThrownBy(() -> features.effects().add(NailEffect.OMBRE))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsConfidenceOutsideTheClosedUnitInterval() {
    assertThatThrownBy(
            () ->
                new NailDesignFeatures(
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of("shape", 1.01),
                    List.of(),
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Confidence for field 'shape' must be between 0 and 1");
  }

  @Test
  void rejectsBlankAmbiguityReasons() {
    assertThatThrownBy(
            () ->
                new NailDesignFeatures(
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    Map.of(),
                    List.of(" "),
                    true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ambiguity reason must not be blank");
  }
}
