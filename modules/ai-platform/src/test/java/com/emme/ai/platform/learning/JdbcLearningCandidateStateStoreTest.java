package com.emme.ai.platform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.learning.LearningCandidateStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class JdbcLearningCandidateStateStoreTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID CANDIDATE_ID = UUID.randomUUID();

  @Test
  void findsCandidateStateUsingTheAuthenticatedTenant() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<LearningCandidateState> result =
        mock(JdbcClient.MappedQuerySpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.query(any(RowMapper.class))).thenReturn(result);
    LearningCandidateState state =
        new LearningCandidateState(CANDIDATE_ID, LearningCandidateStatus.APPROVED, 7);
    when(result.list()).thenReturn(List.of(state));
    JdbcLearningCandidateStateStore store = new JdbcLearningCandidateStateStore(jdbc);

    Optional<LearningCandidateState> found =
        AiExecutionContextScope.call(context(), () -> store.find(CANDIDATE_ID));

    assertThat(found).contains(state);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("FROM ai_learning_candidate")
        .contains("id = :candidateId")
        .contains("tenant_id = :tenantId");
    verify(statement).param("candidateId", CANDIDATE_ID);
    verify(statement).param("tenantId", TENANT_ID);
  }

  @Test
  void updatesOnlyTheExpectedTenantStateAndVersion() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcLearningCandidateStateStore store = new JdbcLearningCandidateStateStore(jdbc);

    boolean changed =
        AiExecutionContextScope.call(
            context(),
            () ->
                store.transition(
                    CANDIDATE_ID,
                    LearningCandidateStatus.APPROVED,
                    7,
                    LearningCandidateStatus.PROMOTED,
                    "promotion passed canary"));

    assertThat(changed).isTrue();
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("UPDATE ai_learning_candidate")
        .contains("status = :expectedStatus")
        .contains("version = :expectedVersion");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("expectedStatus", "APPROVED");
    verify(statement).param("expectedVersion", 7L);
  }

  @Test
  void refusesDatabaseAccessWithoutAnAiContext() {
    JdbcLearningCandidateStateStore store =
        new JdbcLearningCandidateStateStore(mock(JdbcClient.class));

    assertThatThrownBy(() -> store.find(CANDIDATE_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        TENANT_ID,
        UUID.randomUUID(),
        java.util.Set.of("ROLE_owner"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-learning",
        "idem-learning");
  }
}
