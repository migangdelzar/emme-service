package com.emme.ai.platform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.learning.LearningCandidate;
import com.emme.ai.contracts.learning.LearningCandidateEvidence;
import com.emme.ai.contracts.learning.LearningCandidateKind;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class JdbcLearningCandidateStoreTest {

  private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID PRINCIPAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID CONVERSATION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000003");
  private static final UUID WORKFLOW_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

  @Test
  void persistsCandidatesWithBackendCorrelationAndPendingStatus() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<UUID> result = mock(JdbcClient.MappedQuerySpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.query(any(RowMapper.class))).thenReturn(result);
    UUID candidateId = UUID.randomUUID();
    when(result.single()).thenReturn(candidateId);
    JdbcLearningCandidateStore store = new JdbcLearningCandidateStore(jdbc, new ObjectMapper());

    UUID saved = AiExecutionContextScope.call(context(), () -> store.save(candidate(), context()));

    assertThat(saved).isEqualTo(candidateId);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("INSERT INTO ai_learning_candidate")
        .contains("PENDING_EVALUATION")
        .contains("reference_fingerprint")
        .contains("RETURNING id");
    assertThat(sql.getValue())
        .containsPattern("(?s)ON CONFLICT\\s*\\(\\s*tenant_id,\\s*principal_id,\\s*candidate_key");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
    verify(statement).param("conversationId", CONVERSATION_ID);
    verify(statement).param("workflowId", WORKFLOW_ID);
    verify(statement).param("traceId", "trace-1");
    verify(statement).param("candidateKey", "intent:es-MX:service-information");
    verify(statement).param("candidateKind", "INTENT_EXAMPLE");
    verify(statement).param("locale", "es-MX");
    verify(statement).param("embeddingModelVersion", "embeddinggemma:1");
  }

  @Test
  void rejectsAContextArgumentThatDoesNotMatchTheBoundBackendContext() {
    JdbcLearningCandidateStore store =
        new JdbcLearningCandidateStore(mock(JdbcClient.class), new ObjectMapper());

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(), () -> store.save(candidate(), differentContext())))
        .isInstanceOf(SecurityException.class)
        .hasMessage("Learning candidate context does not match the bound AI context");
  }

  @Test
  void refusesPersistenceWithoutTheBackendExecutionContext() {
    JdbcLearningCandidateStore store =
        new JdbcLearningCandidateStore(mock(JdbcClient.class), new ObjectMapper());

    assertThatThrownBy(() -> store.save(candidate(), context()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  private static LearningCandidate candidate() {
    return new LearningCandidate(
        "intent:es-MX:service-information",
        LearningCandidateKind.INTENT_EXAMPLE,
        "what services do you offer?",
        "es-MX",
        "embeddinggemma:1",
        new LearningCandidateEvidence(true, true, true, true, false, false, true));
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("ROLE_CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-1",
        "idem-1");
  }

  private static AiExecutionContext differentContext() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        PRINCIPAL_ID,
        Set.of("ROLE_CLIENT"),
        CONVERSATION_ID,
        WORKFLOW_ID,
        "trace-1",
        "idem-1");
  }
}
