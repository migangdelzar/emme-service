package com.emme.ai.contracts.learning;

import java.util.Objects;

/** Offline evaluation gates required before a candidate can be promoted. */
public record LearningCandidateEvaluation(
    String evaluationVersion,
    boolean datasetComplete,
    boolean safetyPassed,
    boolean regressionPassed,
    boolean shadowComparisonPassed,
    boolean canaryPassed) {

  public LearningCandidateEvaluation {
    Objects.requireNonNull(evaluationVersion, "evaluationVersion must not be null");
    if (evaluationVersion.isBlank()) {
      throw new IllegalArgumentException("evaluationVersion must not be blank");
    }
  }
}
