package com.emme.ai.platform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.learning.LearningCandidateEvaluationReport;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class JdbcLearningCandidateEvaluationStoreTest {

  private static final UUID CANDIDATE_ID = UUID.randomUUID();
  private static final AiExecutionContext CONTEXT =
      new AiExecutionContext(
          UUID.randomUUID(),
          UUID.randomUUID(),
          Set.of("ROLE_SYSTEM_EVALUATOR"),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "trace-eval",
          "idempotency-eval");

  @Test
  void persistsEvaluationEvidenceWithTheBoundTenantAndCandidate() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<UUID> result = mock(JdbcClient.MappedQuerySpec.class);
    UUID evaluationId = UUID.randomUUID();
    when(jdbc.sql(org.mockito.ArgumentMatchers.anyString())).thenReturn(statement);
    when(statement.param(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(statement);
    when(statement.query(org.mockito.ArgumentMatchers.any(RowMapper.class))).thenReturn(result);
    when(result.single()).thenReturn(evaluationId);
    LearningCandidateEvaluationReport report = report();
    JdbcLearningCandidateEvaluationStore store =
        new JdbcLearningCandidateEvaluationStore(jdbc, new ObjectMapper());

    UUID saved =
        AiExecutionContextScope.call(CONTEXT, () -> store.save(CANDIDATE_ID, report, CONTEXT));

    assertThat(saved).isEqualTo(evaluationId);
    org.mockito.ArgumentCaptor<String> sql = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("INSERT INTO ai_learning_candidate_evaluation")
        .contains("CAST(:metrics AS jsonb)")
        .contains("ON CONFLICT (tenant_id, candidate_id, evaluation_version)");
    verify(statement).param("tenantId", CONTEXT.tenantId());
    verify(statement).param("candidateId", CANDIDATE_ID);
    verify(statement).param("evaluationVersion", "ragas-0.4.3");
    verify(statement).param("metrics", "{\"faithfulness\":0.95}");
  }

  private static LearningCandidateEvaluationReport report() {
    return new LearningCandidateEvaluationReport(
        "ragas-0.4.3", java.util.Map.of("faithfulness", 0.95), true, true, true, true, false);
  }
}
