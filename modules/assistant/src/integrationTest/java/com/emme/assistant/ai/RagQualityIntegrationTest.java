package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.rag.KnowledgeQuery;
import com.emme.ai.contracts.rag.KnowledgeRetriever;
import com.emme.ai.contracts.rag.RetrievedDocument;
import com.emme.assistant.ai.application.port.out.RagAnswerPort;
import com.emme.assistant.ai.application.rag.GroundedAnswer;
import com.emme.assistant.ai.application.rag.KnowledgeAnswerService;
import com.emme.assistant.ai.application.rag.KnowledgeRoute;
import com.emme.assistant.ai.application.rag.QueryImprovementPolicy;
import com.emme.assistant.ai.application.rag.QueryImprover;
import com.emme.assistant.ai.application.rag.RetrievalQualityDecision;
import com.emme.assistant.ai.application.rag.RetrievalQualityGate;
import com.emme.assistant.ai.application.rag.RetrievalQualityPolicy;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(RagQualityIntegrationTest.Configuration.class)
class RagQualityIntegrationTest {

  private static final KnowledgeQuery QUERY = new KnowledgeQuery("What are the hours?", "es-MX", 5);
  private static final AiExecutionContext CONTEXT = context();
  private static final RetrievalQualityPolicy QUALITY_POLICY =
      new RetrievalQualityPolicy(0.75, 0.10, 1, Duration.ofDays(30), false);

  @Autowired private KnowledgeAnswerService answerService;
  @Autowired private KnowledgeRetriever retrieval;
  @Autowired private RetrievalQualityGate qualityGate;
  @Autowired private RagAnswerPort answer;

  @BeforeEach
  void resetSharedTestDoubles() {
    org.mockito.Mockito.reset(retrieval, qualityGate, answer);
  }

  @Test
  void generatesOnlyFromAnAcceptedTenantScopedRetrieval() {
    List<RetrievedDocument> documents =
        List.of(new RetrievedDocument("hours", "We open at nine.", Map.of(), 0.92));
    RetrievalQualityDecision accepted =
        new RetrievalQualityDecision(true, 0.92, 0.80, 0.12, 1, 1, true, "ACCEPTED");
    when(retrieval.search(QUERY, CONTEXT)).thenReturn(documents);
    when(qualityGate.evaluate(KnowledgeRoute.GENERAL, QUERY.text(), documents, QUALITY_POLICY))
        .thenReturn(accepted);
    when(answer.answer(QUERY, documents, CONTEXT)).thenReturn("We open at nine.");

    GroundedAnswer result =
        AiExecutionContextScope.call(
            CONTEXT, () -> answerService.answer(QUERY, KnowledgeRoute.GENERAL, CONTEXT));

    assertThat(result.grounded()).isTrue();
    assertThat(result.text()).isEqualTo("We open at nine.");
    verify(answer).answer(QUERY, documents, CONTEXT);
  }

  @Test
  void refusesGenerationWhenTheQualityGateRejectsEveryBoundedAttempt() {
    RetrievalQualityDecision rejected =
        new RetrievalQualityDecision(false, 0.40, 0.35, 0.05, 0, 0, false, "INSUFFICIENT_SUPPORT");
    when(retrieval.search(any(KnowledgeQuery.class), any())).thenReturn(List.of());
    when(qualityGate.evaluate(any(), any(), any(), any())).thenReturn(rejected);

    GroundedAnswer result =
        AiExecutionContextScope.call(
            CONTEXT, () -> answerService.answer(QUERY, KnowledgeRoute.GENERAL, CONTEXT));

    assertThat(result.grounded()).isFalse();
    assertThat(result.text()).isEqualTo("No relevant documents were found.");
    verify(answer, never()).answer(any(), any(), any());
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Configuration {

    @Bean
    KnowledgeRetriever retrieval() {
      return mock(KnowledgeRetriever.class);
    }

    @Bean
    RetrievalQualityGate qualityGate() {
      return mock(RetrievalQualityGate.class);
    }

    @Bean
    QueryImprover queryImprover() {
      return (original, route, previous, context, policy) -> List.of();
    }

    @Bean
    RagAnswerPort answer() {
      return mock(RagAnswerPort.class);
    }

    @Bean
    KnowledgeAnswerService answerService(
        KnowledgeRetriever retrieval,
        RetrievalQualityGate qualityGate,
        QueryImprover queryImprover,
        RagAnswerPort answer) {
      return new KnowledgeAnswerService(
          retrieval,
          qualityGate,
          queryImprover,
          answer,
          QUALITY_POLICY,
          new QueryImprovementPolicy(2, 1, 200, Duration.ofSeconds(1), false, false, false, false));
    }
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
}
