package com.emme.assistant.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeRetriever;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.provider.RetrievalUnavailableException;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeAnswerServiceTest {

  private static final KnowledgeQuery QUERY = new KnowledgeQuery("What are the hours?", "es-MX", 5);
  private static final KnowledgeRoute ROUTE = KnowledgeRoute.FAQ;
  private static final AiExecutionContext CONTEXT = context();
  private static final RetrievalQualityPolicy QUALITY_POLICY =
      new RetrievalQualityPolicy(0.75, 0.10, 1, Duration.ofDays(30), false);
  private static final QueryImprovementPolicy IMPROVEMENT_POLICY =
      new QueryImprovementPolicy(3, 2, 200, Duration.ofSeconds(1), true, true, false, true);

  @Test
  void answersOnlyAfterTheOriginalRetrievalPassesTheQualityGate() {
    KnowledgeRetriever retrieval = org.mockito.Mockito.mock(KnowledgeRetriever.class);
    RetrievalQualityGate gate = org.mockito.Mockito.mock(RetrievalQualityGate.class);
    QueryImprover improver = org.mockito.Mockito.mock(QueryImprover.class);
    RagAnswerPort answer = org.mockito.Mockito.mock(RagAnswerPort.class);
    List<RetrievedDocument> documents = documents();
    RetrievalQualityDecision accepted = acceptedDecision();
    when(retrieval.search(QUERY, CONTEXT)).thenReturn(documents);
    when(gate.evaluate(ROUTE, QUERY.text(), documents, QUALITY_POLICY)).thenReturn(accepted);
    when(answer.answer(QUERY, documents, CONTEXT)).thenReturn("We are open today.");

    KnowledgeAnswerService service =
        new KnowledgeAnswerService(
            retrieval, gate, improver, answer, QUALITY_POLICY, IMPROVEMENT_POLICY);

    GroundedAnswer result =
        AiExecutionContextScope.call(CONTEXT, () -> service.answer(QUERY, ROUTE, CONTEXT));

    assertThat(result.text()).isEqualTo("We are open today.");
    assertThat(result.grounded()).isTrue();
    assertThat(result.sourceIds()).containsExactly("faq-a", "faq-b");
    verify(retrieval).search(QUERY, CONTEXT);
    verify(answer).answer(QUERY, documents, CONTEXT);
    verifyNoInteractions(improver);
  }

  @Test
  void limitsLowConfidenceRetrievalToTheConfiguredAttemptBudget() {
    KnowledgeRetriever retrieval = org.mockito.Mockito.mock(KnowledgeRetriever.class);
    RetrievalQualityGate gate = org.mockito.Mockito.mock(RetrievalQualityGate.class);
    QueryImprover improver = org.mockito.Mockito.mock(QueryImprover.class);
    RagAnswerPort answer = org.mockito.Mockito.mock(RagAnswerPort.class);
    RetrievalQualityDecision rejected = rejectedDecision("INSUFFICIENT_SUPPORT");
    when(retrieval.search(any(KnowledgeQuery.class), any())).thenReturn(List.of());
    when(gate.evaluate(any(), any(), any(), any())).thenReturn(rejected);
    when(improver.improve(QUERY.text(), ROUTE, rejected, CONTEXT, IMPROVEMENT_POLICY))
        .thenReturn(List.of("hours today", "opening hours"));

    KnowledgeAnswerService service =
        new KnowledgeAnswerService(
            retrieval, gate, improver, answer, QUALITY_POLICY, IMPROVEMENT_POLICY);

    GroundedAnswer result =
        AiExecutionContextScope.call(CONTEXT, () -> service.answer(QUERY, ROUTE, CONTEXT));

    assertThat(result.grounded()).isFalse();
    assertThat(result.retrieval().reasonCode()).isEqualTo("INSUFFICIENT_SUPPORT");
    verify(retrieval, org.mockito.Mockito.times(3)).search(any(KnowledgeQuery.class), any());
    verify(answer, never()).answer(any(), any(), any());
  }

  @Test
  void stopsAfterTheFirstImprovedQueryPassesTheQualityGate() {
    KnowledgeRetriever retrieval = org.mockito.Mockito.mock(KnowledgeRetriever.class);
    RetrievalQualityGate gate = org.mockito.Mockito.mock(RetrievalQualityGate.class);
    QueryImprover improver = org.mockito.Mockito.mock(QueryImprover.class);
    RagAnswerPort answer = org.mockito.Mockito.mock(RagAnswerPort.class);
    List<RetrievedDocument> documents = documents();
    RetrievalQualityDecision rejected = rejectedDecision("INSUFFICIENT_SUPPORT");
    RetrievalQualityDecision accepted = acceptedDecision();
    when(retrieval.search(any(KnowledgeQuery.class), any()))
        .thenReturn(List.of())
        .thenReturn(documents);
    when(gate.evaluate(any(), any(), any(), any())).thenReturn(rejected, accepted);
    when(improver.improve(QUERY.text(), ROUTE, rejected, CONTEXT, IMPROVEMENT_POLICY))
        .thenReturn(List.of("opening hours"));
    when(answer.answer(any(), any(), any())).thenReturn("We are open today.");

    KnowledgeAnswerService service =
        new KnowledgeAnswerService(
            retrieval, gate, improver, answer, QUALITY_POLICY, IMPROVEMENT_POLICY);

    GroundedAnswer result =
        AiExecutionContextScope.call(CONTEXT, () -> service.answer(QUERY, ROUTE, CONTEXT));

    assertThat(result.grounded()).isTrue();
    verify(retrieval, org.mockito.Mockito.times(2)).search(any(KnowledgeQuery.class), any());
    verify(answer).answer(any(), any(), any());
  }

  @Test
  void returnsAProviderFallbackWithoutEnteringTheRewriteLoop() {
    KnowledgeRetriever retrieval = org.mockito.Mockito.mock(KnowledgeRetriever.class);
    RetrievalQualityGate gate = org.mockito.Mockito.mock(RetrievalQualityGate.class);
    QueryImprover improver = org.mockito.Mockito.mock(QueryImprover.class);
    RagAnswerPort answer = org.mockito.Mockito.mock(RagAnswerPort.class);
    when(retrieval.search(QUERY, CONTEXT)).thenThrow(new RetrievalUnavailableException());

    KnowledgeAnswerService service =
        new KnowledgeAnswerService(
            retrieval, gate, improver, answer, QUALITY_POLICY, IMPROVEMENT_POLICY);

    GroundedAnswer result =
        AiExecutionContextScope.call(CONTEXT, () -> service.answer(QUERY, ROUTE, CONTEXT));

    assertThat(result.text()).isEqualTo("Retrieval unavailable.");
    assertThat(result.grounded()).isFalse();
    verifyNoInteractions(gate, improver, answer);
  }

  @Test
  void rejectsAContextThatDoesNotMatchTheCurrentExecutionScope() {
    KnowledgeRetriever retrieval = org.mockito.Mockito.mock(KnowledgeRetriever.class);
    RetrievalQualityGate gate = org.mockito.Mockito.mock(RetrievalQualityGate.class);
    QueryImprover improver = org.mockito.Mockito.mock(QueryImprover.class);
    RagAnswerPort answer = org.mockito.Mockito.mock(RagAnswerPort.class);
    AiExecutionContext otherContext = context();
    KnowledgeAnswerService service =
        new KnowledgeAnswerService(
            retrieval, gate, improver, answer, QUALITY_POLICY, IMPROVEMENT_POLICY);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    CONTEXT, () -> service.answer(QUERY, ROUTE, otherContext)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("context must match the current AI execution context");
    verifyNoInteractions(retrieval, gate, improver, answer);
  }

  @Test
  void stopsImprovementWhenTheDurationBudgetIsExhausted() {
    KnowledgeRetriever retrieval = org.mockito.Mockito.mock(KnowledgeRetriever.class);
    RetrievalQualityGate gate = org.mockito.Mockito.mock(RetrievalQualityGate.class);
    QueryImprover improver = org.mockito.Mockito.mock(QueryImprover.class);
    RagAnswerPort answer = org.mockito.Mockito.mock(RagAnswerPort.class);
    MutableClock clock = new MutableClock(Instant.parse("2026-09-05T12:00:00Z"));
    QueryImprovementPolicy policy =
        new QueryImprovementPolicy(3, 2, 200, Duration.ofSeconds(1), true, true, false, true);
    RetrievalQualityDecision rejected = rejectedDecision("INSUFFICIENT_SUPPORT");
    when(retrieval.search(any(KnowledgeQuery.class), any())).thenReturn(List.of());
    when(gate.evaluate(any(), any(), any(), any())).thenReturn(rejected);
    when(improver.improve(QUERY.text(), ROUTE, rejected, CONTEXT, policy))
        .thenAnswer(
            invocation -> {
              clock.advance(Duration.ofSeconds(2));
              return List.of("opening hours");
            });

    KnowledgeAnswerService service =
        new KnowledgeAnswerService(
            retrieval, gate, improver, answer, QUALITY_POLICY, policy, clock);

    GroundedAnswer result =
        AiExecutionContextScope.call(CONTEXT, () -> service.answer(QUERY, ROUTE, CONTEXT));

    assertThat(result.grounded()).isFalse();
    verify(retrieval).search(any(KnowledgeQuery.class), any());
    verify(improver).improve(QUERY.text(), ROUTE, rejected, CONTEXT, policy);
    verify(answer, never()).answer(any(), any(), any());
  }

  @Test
  void doesNotScheduleVariantsWhenTheVariantBudgetIsZero() {
    KnowledgeRetriever retrieval = org.mockito.Mockito.mock(KnowledgeRetriever.class);
    RetrievalQualityGate gate = org.mockito.Mockito.mock(RetrievalQualityGate.class);
    QueryImprover improver = org.mockito.Mockito.mock(QueryImprover.class);
    RagAnswerPort answer = org.mockito.Mockito.mock(RagAnswerPort.class);
    QueryImprovementPolicy policy =
        new QueryImprovementPolicy(3, 0, 200, Duration.ofSeconds(1), true, true, false, true);
    RetrievalQualityDecision rejected = rejectedDecision("INSUFFICIENT_SUPPORT");
    when(retrieval.search(any(KnowledgeQuery.class), any())).thenReturn(List.of());
    when(gate.evaluate(any(), any(), any(), any())).thenReturn(rejected);
    when(improver.improve(QUERY.text(), ROUTE, rejected, CONTEXT, policy))
        .thenReturn(List.of("opening hours"));

    KnowledgeAnswerService service =
        new KnowledgeAnswerService(retrieval, gate, improver, answer, QUALITY_POLICY, policy);

    GroundedAnswer result =
        AiExecutionContextScope.call(CONTEXT, () -> service.answer(QUERY, ROUTE, CONTEXT));

    assertThat(result.grounded()).isFalse();
    verify(retrieval).search(any(KnowledgeQuery.class), any());
    verify(improver).improve(QUERY.text(), ROUTE, rejected, CONTEXT, policy);
    verify(answer, never()).answer(any(), any(), any());
  }

  @Test
  void preservesNonProviderRetrievalFailures() {
    KnowledgeRetriever retrieval = org.mockito.Mockito.mock(KnowledgeRetriever.class);
    RetrievalQualityGate gate = org.mockito.Mockito.mock(RetrievalQualityGate.class);
    QueryImprover improver = org.mockito.Mockito.mock(QueryImprover.class);
    RagAnswerPort answer = org.mockito.Mockito.mock(RagAnswerPort.class);
    when(retrieval.search(QUERY, CONTEXT)).thenThrow(new IllegalArgumentException("bad query"));

    KnowledgeAnswerService service =
        new KnowledgeAnswerService(
            retrieval, gate, improver, answer, QUALITY_POLICY, IMPROVEMENT_POLICY);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(CONTEXT, () -> service.answer(QUERY, ROUTE, CONTEXT)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("bad query");
    verifyNoInteractions(gate, improver, answer);
  }

  private static List<RetrievedDocument> documents() {
    return List.of(
        new RetrievedDocument("faq-a", "We open at nine.", Map.of(), 0.92),
        new RetrievedDocument("faq-b", "We close at six.", Map.of(), 0.80));
  }

  private static RetrievalQualityDecision acceptedDecision() {
    return new RetrievalQualityDecision(true, 0.92, 0.80, 0.12, 2, 2, true, "ACCEPTED");
  }

  private static RetrievalQualityDecision rejectedDecision(String reason) {
    return new RetrievalQualityDecision(false, 0.50, 0.45, 0.05, 0, 0, false, reason);
  }

  private static AiExecutionContext context() {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("ROLE_tenant_client"),
        id,
        id,
        "trace",
        "idempotency");
  }

  private static final class MutableClock extends Clock {

    private Instant current;

    private MutableClock(Instant current) {
      this.current = current;
    }

    private void advance(Duration duration) {
      current = current.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return current;
    }
  }
}
