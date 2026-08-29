package com.emme.ai.contracts.learning;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LearningCandidateTest {

  @Test
  void rejectsCommonPiiFromReferenceTextBeforeCandidateAdmission() {
    assertThatThrownBy(() -> candidate("contact ana@example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("referenceText must be PII-redacted");
  }

  @Test
  void rejectsReferenceTextLongerThanTheDurableCandidateColumn() {
    assertThatThrownBy(() -> candidate("x".repeat(4001)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("referenceText must not exceed 4000 characters");
  }

  private static LearningCandidate candidate(String referenceText) {
    return new LearningCandidate(
        "intent:es-MX:service-information",
        LearningCandidateKind.INTENT_EXAMPLE,
        referenceText,
        "es-MX",
        "embeddinggemma:1",
        new LearningCandidateEvidence(true, true, true, true, false, false, true));
  }
}
