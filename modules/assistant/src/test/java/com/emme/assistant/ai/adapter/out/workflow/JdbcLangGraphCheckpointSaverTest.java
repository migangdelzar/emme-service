package com.emme.assistant.ai.adapter.out.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class JdbcLangGraphCheckpointSaverTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void listsCheckpointsUsingTheAuthenticatedTenantAndWorkflow() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<Checkpoint> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    Checkpoint checkpoint = checkpoint("checkpoint-1");
    when(result.list()).thenReturn(List.of(checkpoint));
    JdbcLangGraphCheckpointSaver saver = new JdbcLangGraphCheckpointSaver(jdbc, new ObjectMapper());

    List<Checkpoint> checkpoints =
        AiExecutionContextScope.call(context(), () -> List.copyOf(saver.list(config())));

    assertThat(checkpoints).containsExactly(checkpoint);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc, atLeast(1)).sql(sql.capture());
    assertThat(sql.getAllValues())
        .anySatisfy(
            value ->
                assertThat(value)
                    .contains("FROM ai_workflow_checkpoint")
                    .contains("tenant_id = :tenantId")
                    .contains("workflow_id = :workflowId")
                    .contains("conversation_id = :conversationId")
                    .contains("principal_id = :principalId")
                    .contains("ORDER BY checkpoint.created_at"));
    verify(statement, atLeast(1)).param("tenantId", TENANT_ID);
    verify(statement, atLeast(1)).param("workflowId", WORKFLOW_ID);
  }

  @Test
  void persistsCheckpointStateAndReturnsItsCheckpointIdForResume() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<String> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    when(result.single()).thenReturn("checkpoint-1");
    JdbcLangGraphCheckpointSaver saver = new JdbcLangGraphCheckpointSaver(jdbc, new ObjectMapper());
    Checkpoint checkpoint = checkpoint("checkpoint-1");

    RunnableConfig returned =
        AiExecutionContextScope.call(context(), () -> saver.put(config(), checkpoint));

    assertThat(returned.threadId()).contains(WORKFLOW_ID.toString());
    assertThat(returned.checkPointId()).contains("checkpoint-1");
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc, atLeast(3)).sql(sql.capture());
    assertThat(sql.getAllValues())
        .anySatisfy(
            value ->
                assertThat(value)
                    .contains("INSERT INTO ai_workflow_checkpoint")
                    .contains("workflow_namespace")
                    .contains("next_node_name")
                    .contains(
                        "ON CONFLICT (tenant_id, workflow_id, workflow_namespace, node_name,"
                            + " node_execution_key)")
                    .contains("RETURNING node_execution_key"));
    verify(statement, atLeast(1)).param("tenantId", TENANT_ID);
    verify(statement, atLeast(1)).param("workflowId", WORKFLOW_ID);
    verify(statement).param("nodeExecutionKey", "checkpoint-1");
  }

  @Test
  void loadsOneCheckpointWhenTheRunnableConfigContainsItsCheckpointId() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<Checkpoint> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    Checkpoint checkpoint = checkpoint("checkpoint-1");
    when(result.list()).thenReturn(List.of(checkpoint));
    JdbcLangGraphCheckpointSaver saver = new JdbcLangGraphCheckpointSaver(jdbc, new ObjectMapper());
    RunnableConfig config =
        RunnableConfig.builder()
            .threadId(WORKFLOW_ID.toString())
            .checkPointId("checkpoint-1")
            .build();

    assertThat(
            AiExecutionContextScope.call(context(), () -> saver.get(config)).orElseThrow().getId())
        .isEqualTo(checkpoint.getId());

    verify(statement).param("checkpointId", "checkpoint-1");
  }

  @Test
  void loadsTheLatestCheckpointWhenTheRunnableConfigHasNoCheckpointId() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<Checkpoint> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    Checkpoint checkpoint = checkpoint("checkpoint-1");
    when(result.list()).thenReturn(List.of(checkpoint));
    JdbcLangGraphCheckpointSaver saver = new JdbcLangGraphCheckpointSaver(jdbc, new ObjectMapper());

    assertThat(
            AiExecutionContextScope.call(context(), () -> saver.get(config()))
                .orElseThrow()
                .getId())
        .isEqualTo(checkpoint.getId());
  }

  @Test
  void rejectsAConfigForAnotherWorkflowEvenWhenTheTenantIsAuthenticated() {
    JdbcLangGraphCheckpointSaver saver =
        new JdbcLangGraphCheckpointSaver(mock(JdbcClient.class), new ObjectMapper());
    RunnableConfig foreignConfig =
        RunnableConfig.builder().threadId(UUID.randomUUID().toString()).build();

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(context(), () -> saver.list(foreignConfig)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Checkpoint thread does not match AI workflow context");
  }

  @Test
  void rejectsAWorkflowThreadWithAnEmptyNamespace() {
    JdbcLangGraphCheckpointSaver saver =
        new JdbcLangGraphCheckpointSaver(mock(JdbcClient.class), new ObjectMapper());
    RunnableConfig malformedConfig = RunnableConfig.builder().threadId(WORKFLOW_ID + ":").build();

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(context(), () -> saver.list(malformedConfig)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Checkpoint thread namespace must not be blank");
  }

  private static RunnableConfig config() {
    return RunnableConfig.builder().threadId(WORKFLOW_ID.toString()).build();
  }

  private static Checkpoint checkpoint(String id) {
    return Checkpoint.builder()
        .id(id)
        .state(Map.of("status", "WAITING_FOR_STAFF"))
        .nodeId("wait_for_staff")
        .nextNodeId("approval_gate")
        .build();
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

  private static void stubQuery(
      JdbcClient jdbc, JdbcClient.StatementSpec statement, JdbcClient.MappedQuerySpec<?> result) {
    JdbcClient.MappedQuerySpec<Integer> count = mock(JdbcClient.MappedQuerySpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.query(any(RowMapper.class))).thenReturn(result);
    when(statement.query(Integer.class)).thenReturn(count);
    when(count.single()).thenReturn(1);
  }
}
