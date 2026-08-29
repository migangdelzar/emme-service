package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class JdbcAiToolIdempotencyStoreTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void claimsACommandUsingOnlyTheAuthenticatedTenant() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcAiToolIdempotencyStore store = new JdbcAiToolIdempotencyStore(jdbc, new ObjectMapper());

    boolean claimed =
        AiExecutionContextScope.call(
            context(),
            () ->
                store.claim(
                    "createAppointment:" + PRINCIPAL_ID + ":request-1", "createAppointment"));

    assertThat(claimed).isTrue();
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("INSERT INTO ai_tool_idempotency")
        .contains("tenant_id")
        .contains("operation_key")
        .contains("ON CONFLICT (tenant_id, principal_id, operation_key)");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
    verify(statement).param("operationKey", "createAppointment:" + PRINCIPAL_ID + ":request-1");
    verify(statement).param("toolKey", "createAppointment");
  }

  @Test
  void reclaimsOnlyAnExpiredInProgressClaim() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcAiToolIdempotencyStore store =
        new JdbcAiToolIdempotencyStore(jdbc, new ObjectMapper(), Duration.ofMinutes(10));

    boolean claimed =
        AiExecutionContextScope.call(
            context(),
            () ->
                store.claim(
                    "createAppointment:" + PRINCIPAL_ID + ":request-1", "createAppointment"));

    assertThat(claimed).isTrue();
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("lease_expires_at")
        .contains("CURRENT_TIMESTAMP")
        .contains("status = 'IN_PROGRESS'")
        .contains("lease_expires_at <= CURRENT_TIMESTAMP")
        .contains("DO UPDATE SET")
        .contains("WHERE ai_tool_idempotency.status = 'IN_PROGRESS'");
    verify(statement).param("claimLeaseSeconds", 600L);
  }

  @Test
  void findsOnlyCompletedResultsWithinTheAuthenticatedTenant() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<AiToolResult> result = mock(JdbcClient.MappedQuerySpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.query(any(RowMapper.class))).thenReturn(result);
    AiToolResult stored = new AiToolResult("createAppointment", "appointment-created", true);
    when(result.list()).thenReturn(List.of(stored));
    JdbcAiToolIdempotencyStore store = new JdbcAiToolIdempotencyStore(jdbc, new ObjectMapper());

    var found =
        AiExecutionContextScope.call(
            context(), () -> store.find("createAppointment:" + PRINCIPAL_ID + ":request-1"));

    assertThat(found).contains(stored);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("SELECT result_payload")
        .contains("tenant_id = :tenantId")
        .contains("operation_key = :operationKey")
        .contains("status = 'SUCCEEDED'");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
  }

  @Test
  void completesAndReleasesOnlyTenantScopedInProgressCommands() throws Exception {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcAiToolIdempotencyStore store = new JdbcAiToolIdempotencyStore(jdbc, new ObjectMapper());
    AiToolResult completed = new AiToolResult("createAppointment", "appointment-created", true);

    AiExecutionContextScope.run(
        context(),
        () -> {
          String operationKey = "createAppointment:" + PRINCIPAL_ID + ":request-1";
          store.complete(operationKey, completed);
          store.release(operationKey);
        });

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc, org.mockito.Mockito.times(2)).sql(sql.capture());
    assertThat(sql.getAllValues().get(0))
        .contains("UPDATE ai_tool_idempotency")
        .contains("status = 'IN_PROGRESS'")
        .contains("lease_expires_at = NULL")
        .contains("result_payload = CAST(:resultPayload AS jsonb)")
        .contains("tenant_id = :tenantId")
        .contains("principal_id = :principalId");
    assertThat(sql.getAllValues().get(1))
        .contains("DELETE FROM ai_tool_idempotency")
        .contains("status = 'IN_PROGRESS'")
        .contains("tenant_id = :tenantId")
        .contains("principal_id = :principalId");
    verify(statement).param("resultPayload", new ObjectMapper().writeValueAsString(completed));
  }

  @Test
  void refusesDatabaseAccessWithoutTheBackendExecutionContext() {
    JdbcAiToolIdempotencyStore store =
        new JdbcAiToolIdempotencyStore(mock(JdbcClient.class), new ObjectMapper());

    assertThatThrownBy(() -> store.find("createAppointment:request-1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
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
}
