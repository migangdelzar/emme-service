package com.emme.ai.contracts.extraction;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Closed, immutable structured result from nail-design extraction. */
public record NailDesignFeatures(
    NailShape shape,
    NailLength length,
    String baseColor,
    List<NailEffect> effects,
    List<NailDecoration> decorations,
    ExtensionType extensionType,
    Boolean removalRequired,
    Boolean repairRequired,
    ArtComplexity artComplexity,
    Map<String, Double> confidenceByField,
    List<String> ambiguities,
    boolean needsHumanReview) {

  public NailDesignFeatures {
    if (baseColor != null && baseColor.isBlank()) {
      throw new IllegalArgumentException("baseColor must not be blank when present");
    }
    effects = immutableList(effects, "effects");
    decorations = immutableList(decorations, "decorations");
    confidenceByField = immutableConfidence(confidenceByField);
    ambiguities = immutableReasons(ambiguities);
  }

  private static <T> List<T> immutableList(List<T> values, String field) {
    Objects.requireNonNull(values, field + " must not be null");
    if (values.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(field + " must not contain null values");
    }
    return List.copyOf(values);
  }

  private static Map<String, Double> immutableConfidence(Map<String, Double> values) {
    Objects.requireNonNull(values, "confidenceByField must not be null");
    values.forEach(
        (field, confidence) -> {
          if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("confidence field must not be blank");
          }
          if (confidence == null
              || !Double.isFinite(confidence)
              || confidence < 0
              || confidence > 1) {
            throw new IllegalArgumentException(
                "Confidence for field '" + field + "' must be between 0 and 1");
          }
        });
    return Map.copyOf(values);
  }

  private static List<String> immutableReasons(List<String> reasons) {
    Objects.requireNonNull(reasons, "ambiguities must not be null");
    if (reasons.stream().anyMatch(reason -> reason == null || reason.isBlank())) {
      throw new IllegalArgumentException("ambiguity reason must not be blank");
    }
    return List.copyOf(reasons);
  }
}
