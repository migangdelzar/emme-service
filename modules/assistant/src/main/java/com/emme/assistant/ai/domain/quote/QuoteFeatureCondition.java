package com.emme.assistant.ai.domain.quote;

import java.util.Objects;

/** Serializable, closed-world condition used by deterministic quote calculation. */
public record QuoteFeatureCondition(QuoteFeatureConditionKind kind, String expectedValue) {

  public QuoteFeatureCondition {
    Objects.requireNonNull(kind, "kind must not be null");
    if (kind == QuoteFeatureConditionKind.ALWAYS) {
      if (expectedValue != null) {
        throw new IllegalArgumentException("ALWAYS condition must not define an expected value");
      }
    } else if (expectedValue == null || expectedValue.isBlank()) {
      throw new IllegalArgumentException("Conditional quote line must define an expected value");
    }
  }

  public static QuoteFeatureCondition always() {
    return new QuoteFeatureCondition(QuoteFeatureConditionKind.ALWAYS, null);
  }

  public static QuoteFeatureCondition forEffect(NailEffect effect) {
    return forEnum(QuoteFeatureConditionKind.EFFECT, effect);
  }

  public static QuoteFeatureCondition forDecoration(NailDecoration decoration) {
    return forEnum(QuoteFeatureConditionKind.DECORATION, decoration);
  }

  public static QuoteFeatureCondition forExtension(ExtensionType extensionType) {
    return forEnum(QuoteFeatureConditionKind.EXTENSION_TYPE, extensionType);
  }

  public static QuoteFeatureCondition forRemoval() {
    return new QuoteFeatureCondition(QuoteFeatureConditionKind.REMOVAL_REQUIRED, "true");
  }

  public static QuoteFeatureCondition forRepair() {
    return new QuoteFeatureCondition(QuoteFeatureConditionKind.REPAIR_REQUIRED, "true");
  }

  public static QuoteFeatureCondition forArtComplexity(ArtComplexity complexity) {
    return forEnum(QuoteFeatureConditionKind.ART_COMPLEXITY, complexity);
  }

  private static QuoteFeatureCondition forEnum(
      QuoteFeatureConditionKind kind, Enum<?> expectedValue) {
    return new QuoteFeatureCondition(kind, Objects.requireNonNull(expectedValue).name());
  }

  public boolean isKnown(NailDesignFeatures features) {
    Objects.requireNonNull(features, "features must not be null");
    return switch (kind) {
      case ALWAYS -> true;
      case EFFECT -> true;
      case DECORATION -> true;
      case EXTENSION_TYPE -> features.extensionType() != null;
      case REMOVAL_REQUIRED -> features.removalRequired() != null;
      case REPAIR_REQUIRED -> features.repairRequired() != null;
      case ART_COMPLEXITY -> features.artComplexity() != null;
    };
  }

  public boolean matches(NailDesignFeatures features) {
    Objects.requireNonNull(features, "features must not be null");
    return switch (kind) {
      case ALWAYS -> true;
      case EFFECT ->
          features.effects().stream().anyMatch(effect -> effect.name().equals(expectedValue));
      case DECORATION ->
          features.decorations().stream()
              .anyMatch(decoration -> decoration.name().equals(expectedValue));
      case EXTENSION_TYPE ->
          features.extensionType() != null && features.extensionType().name().equals(expectedValue);
      case REMOVAL_REQUIRED ->
          features.removalRequired() != null
              && features.removalRequired().toString().equals(expectedValue);
      case REPAIR_REQUIRED ->
          features.repairRequired() != null
              && features.repairRequired().toString().equals(expectedValue);
      case ART_COMPLEXITY ->
          features.artComplexity() != null && features.artComplexity().name().equals(expectedValue);
    };
  }

  public String requiredField() {
    return switch (kind) {
      case ALWAYS, EFFECT, DECORATION -> null;
      case EXTENSION_TYPE -> "extensionType";
      case REMOVAL_REQUIRED -> "removalRequired";
      case REPAIR_REQUIRED -> "repairRequired";
      case ART_COMPLEXITY -> "artComplexity";
    };
  }
}
