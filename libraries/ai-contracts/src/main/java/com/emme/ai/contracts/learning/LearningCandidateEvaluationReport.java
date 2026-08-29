package com.emme.ai.contracts.learning;

import java.util.Map;
import java.util.Objects;

/** Evaluation evidence and quality metrics returned by an offline evaluator. */
public record LearningCandidateEvaluationReport(
    String evaluationVersion,
    Map<String, Double> metrics,
    boolean datasetComplete,
    boolean safetyPassed,
    boolean regressionPassed,
    boolean shadowComparisonPassed,
    boolean canaryPassed) {

  private static final int MAX_METRICS = 128;
  private static final int MAX_METRIC_NAME_LENGTH = 120;

  public LearningCandidateEvaluationReport {
    evaluationVersion = requireText(evaluationVersion, "evaluationVersion", 150);
    metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    if (metrics.size() > MAX_METRICS) {
      throw new IllegalArgumentException("metrics must not contain more than 128 values");
    }
    for (Map.Entry<String, Double> entry : metrics.entrySet()) {
      if (entry.getKey() == null
          || entry.getKey().isBlank()
          || entry.getKey().length() > MAX_METRIC_NAME_LENGTH) {
        throw new IllegalArgumentException("metric names must be bounded and non-blank");
      }
      Double value = entry.getValue();
      if (value == null || !Double.isFinite(value) || value < 0.0 || value > 1.0) {
        throw new IllegalArgumentException("metrics must contain values between 0 and 1");
      }
    }
    metrics = Map.copyOf(metrics);
  }

  public LearningCandidateEvaluation toLifecycleEvaluation() {
    return new LearningCandidateEvaluation(
        evaluationVersion,
        datasetComplete,
        safetyPassed,
        regressionPassed,
        shadowComparisonPassed,
        canaryPassed);
  }

  private static String requireText(String value, String field, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    if (value.length() > maxLength) {
      throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
    }
    return value;
  }
}
