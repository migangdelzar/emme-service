package com.emme.assistant.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.rag.RetrievedDocument;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetrievalQualityGateTest {

  private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
  private static final RetrievalQualityPolicy POLICY =
      new RetrievalQualityPolicy(0.75, 0.10, 2, Duration.ofDays(30), true);

  private final RetrievalQualityGate gate =
      new DeterministicRetrievalQualityGate(Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void rejectsEmptyResultsWithAnExplicitReason() {
    RetrievalQualityDecision decision =
        gate.evaluate(KnowledgeRoute.FAQ, "hours", List.of(), POLICY);

    assertThat(decision.accepted()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("NO_DOCUMENTS");
    assertThat(decision.topScore()).isZero();
    assertThat(decision.supportingDocumentCount()).isZero();
  }

  @Test
  void rejectsOneResultBecauseThereIsNoScoreMargin() {
    RetrievalQualityDecision decision =
        gate.evaluate(
            KnowledgeRoute.FAQ, "hours", List.of(document("faq", 0.95, NOW, true)), POLICY);

    assertThat(decision.accepted()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("INSUFFICIENT_MARGIN");
    assertThat(decision.margin()).isZero();
  }

  @Test
  void rejectsResultsBelowTheTopScoreThreshold() {
    RetrievalQualityDecision decision =
        gate.evaluate(
            KnowledgeRoute.POLICY,
            "refunds",
            List.of(document("policy-a", 0.70, NOW, true), document("policy-b", 0.40, NOW, true)),
            POLICY);

    assertThat(decision.accepted()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("TOP_SCORE_BELOW_THRESHOLD");
  }

  @Test
  void countsIndependentSourceDocumentsRatherThanDuplicateChunks() {
    RetrievalQualityDecision decision =
        gate.evaluate(
            KnowledgeRoute.DESIGN,
            "nail design",
            List.of(document("design-a", 0.95, NOW, true), document("design-a", 0.80, NOW, true)),
            POLICY);

    assertThat(decision.accepted()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("INSUFFICIENT_SUPPORT");
    assertThat(decision.supportingDocumentCount()).isEqualTo(1);
  }

  @Test
  void rejectsWhenTheRequiredSupportingDocumentsAreStale() {
    RetrievalQualityDecision decision =
        gate.evaluate(
            KnowledgeRoute.POLICY,
            "refunds",
            List.of(
                document("policy-a", 0.95, NOW.minus(Duration.ofDays(31)), true),
                document("policy-b", 0.80, NOW.minus(Duration.ofDays(32)), true)),
            POLICY);

    assertThat(decision.accepted()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("STALE_DOCUMENTS");
    assertThat(decision.freshDocumentCount()).isZero();
  }

  @Test
  void rejectsWhenHybridSearchDoesNotHaveLexicalAgreement() {
    RetrievalQualityDecision decision =
        gate.evaluate(
            KnowledgeRoute.FAQ,
            "hours",
            List.of(document("faq-a", 0.95, NOW, false), document("faq-b", 0.80, NOW, true)),
            POLICY);

    assertThat(decision.accepted()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("LEXICAL_DISAGREEMENT");
    assertThat(decision.lexicalAgreement()).isFalse();
  }

  @Test
  void acceptsFiniteScoresAfterSortingAndCalculatingTheTopTwoMargin() {
    RetrievalQualityDecision decision =
        gate.evaluate(
            KnowledgeRoute.GENERAL,
            "hours",
            List.of(document("faq-b", 0.80, NOW, true), document("faq-a", 0.92, NOW, true)),
            POLICY);

    assertThat(decision.accepted()).isTrue();
    assertThat(decision.topScore()).isEqualTo(0.92);
    assertThat(decision.secondScore()).isEqualTo(0.80);
    assertThat(decision.margin()).isEqualTo(0.12);
    assertThat(decision.supportingDocumentCount()).isEqualTo(2);
    assertThat(decision.freshDocumentCount()).isEqualTo(2);
    assertThat(decision.lexicalAgreement()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("ACCEPTED");
  }

  private static RetrievedDocument document(
      String sourceId, double score, Instant effectiveAt, boolean lexicalMatch) {
    return new RetrievedDocument(
        sourceId,
        "trusted test content",
        Map.of(
            "effectiveAt", effectiveAt.toString(), "lexicalMatch", Boolean.toString(lexicalMatch)),
        score);
  }
}
