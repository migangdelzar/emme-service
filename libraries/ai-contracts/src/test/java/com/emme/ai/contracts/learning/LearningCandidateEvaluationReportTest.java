package com.emme.ai.contracts.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LearningCandidateEvaluationReportTest {

  @Test
  void copiesAndValidatesMetricEvidence() {
    Map<String, Double> metrics = Map.of("faithfulness", 0.95, "answer_relevancy", 0.90);

    LearningCandidateEvaluationReport report =
        new LearningCandidateEvaluationReport(
            "ragas-0.4.3", metrics, true, true, true, true, false);

    assertThat(report.metrics()).containsExactlyInAnyOrderEntriesOf(metrics);
    assertThat(report.toLifecycleEvaluation())
        .isEqualTo(new LearningCandidateEvaluation("ragas-0.4.3", true, true, true, true, false));
  }

  @Test
  void rejectsMetricValuesOutsideTheEvidenceRange() {
    assertThatThrownBy(
            () ->
                new LearningCandidateEvaluationReport(
                    "ragas-0.4.3", Map.of("faithfulness", 1.1), true, true, true, true, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("metrics must contain values between 0 and 1");
  }
}
