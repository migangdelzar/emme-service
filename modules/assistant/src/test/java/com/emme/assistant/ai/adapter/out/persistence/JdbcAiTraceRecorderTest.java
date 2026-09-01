package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.trace.AiExecutionStatus;
import com.emme.assistant.ai.application.trace.AiModelExecutionTrace;
import com.emme.assistant.ai.application.trace.AiSemanticExecutionTrace;
import com.emme.assistant.ai.application.trace.AiToolCallStatus;
import com.emme.assistant.ai.application.trace.AiToolCallTrace;
import com.emme.assistant.ai.application.trace.AiTraceRedactor;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcAiTraceRecorderTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Test
  void persistsModelExecutionUsingOnlyAuthenticatedContextAndRedactedPayloads() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcAiTraceRecorder recorder =
        new JdbcAiTraceRecorder(jdbc, new AiTraceRedactor(), new ObjectMapper());

    AiModelExecutionTrace trace =
        new AiModelExecutionTrace(
            UUID.randomUUID(),
            "CHAT_COMPLETION",
            "local-ollama",
            "gemma-v1",
            "chat-v1",
            null,
            AiExecutionStatus.SUCCEEDED,
            12,
            10,
            4,
            14,
            new java.math.BigDecimal("0.0012"),
            "email=ana@example.com",
            "phone=+52 55 1234 5678",
            null,
            null);

    AiExecutionContextScope.run(context(), () -> recorder.recordModelExecution(trace));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("INSERT INTO ai_model_execution")
        .contains("tenant_id")
        .contains("principal_id")
        .contains("conversation_id")
        .contains("workflow_id")
        .contains("trace_id")
        .contains("input_tokens")
        .contains("estimated_cost")
        .contains("ON CONFLICT (tenant_id, execution_key)");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
    verify(statement).param("conversationId", CONVERSATION_ID);
    verify(statement).param("workflowId", WORKFLOW_ID);
    verify(statement).param("requestPayload", "email=[REDACTED_EMAIL]");
    verify(statement).param("responsePayload", "phone=[REDACTED_PHONE]");
    verify(statement).param("inputTokens", 10);
    verify(statement).param("outputTokens", 4);
    verify(statement).param("totalTokens", 14);
  }

  @Test
  void persistsToolCallWithAuthorizationOutcomeAndAuthenticatedTenant() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcAiTraceRecorder recorder =
        new JdbcAiTraceRecorder(jdbc, new AiTraceRedactor(), new ObjectMapper());

    AiToolCallTrace trace =
        new AiToolCallTrace(
            UUID.randomUUID(),
            "getSalonServices",
            "READ_ONLY",
            AiToolCallStatus.SUCCEEDED,
            true,
            false,
            false,
            4,
            "{\"email\":\"ana@example.com\"}",
            "services",
            null,
            null);

    AiExecutionContextScope.run(context(), () -> recorder.recordToolCall(trace));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("INSERT INTO ai_tool_call")
        .contains("authorized")
        .contains("tool_key")
        .contains("tenant_id");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("argumentsPayload", "{\"email\":\"[REDACTED_EMAIL]\"}");
  }

  @Test
  void persistsSemanticOutcomeScoresMatchesAndInvalidationContext() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcAiTraceRecorder recorder =
        new JdbcAiTraceRecorder(jdbc, new AiTraceRedactor(), new ObjectMapper());

    AiSemanticExecutionTrace trace =
        new AiSemanticExecutionTrace(
            UUID.randomUUID(),
            TENANT_ID,
            PRINCIPAL_ID,
            "routing",
            "accepted",
            0.98,
            0.72,
            0.26,
            java.util.List.of("book_appointment", "availability"),
            "QUOTE_TEMPLATE",
            "template-v4",
            "catalog-item=123",
            7);

    recorder.recordSemanticOutcome(trace);

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("INSERT INTO ai_semantic_execution")
        .contains("top1_similarity")
        .contains("matches")
        .contains("invalidation_context");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("principalId", PRINCIPAL_ID);
    verify(statement).param("top1Similarity", 0.98);
    verify(statement).param("margin", 0.26);
    verify(statement).param("dependency", "QUOTE_TEMPLATE");
    verify(statement).param("dependencyVersion", "template-v4");
  }

  @Test
  void persistsTenantWideInvalidationWithTheSystemAuditActorAndNoConversationForeignKey() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcAiTraceRecorder recorder =
        new JdbcAiTraceRecorder(jdbc, new AiTraceRedactor(), new ObjectMapper());
    AiSemanticExecutionTrace trace =
        new AiSemanticExecutionTrace(
            UUID.randomUUID(),
            TENANT_ID,
            new UUID(0, 0),
            "cache_invalidation",
            "requested",
            0.0,
            0.0,
            0.0,
            java.util.List.of(),
            "PRICE",
            "price-v2",
            "tenant-wide",
            0);

    AiExecutionContextScope.run(context(), () -> recorder.recordSemanticOutcome(trace));

    verify(statement).param("principalId", new UUID(0, 0));
    verify(statement).param("conversationId", null);
    verify(statement).param("workflowId", null);
  }

  @Test
  void refusesToPersistATraceWithoutBackendExecutionContext() {
    JdbcAiTraceRecorder recorder =
        new JdbcAiTraceRecorder(mock(JdbcClient.class), new AiTraceRedactor(), new ObjectMapper());

    assertThatThrownBy(() -> recorder.recordModelExecution(modelTrace()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No AI execution context");
  }

  @Test
  void serializesSemanticMatchesWithJacksonControlCharacterEscaping() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcAiTraceRecorder recorder =
        new JdbcAiTraceRecorder(jdbc, new AiTraceRedactor(), new ObjectMapper());
    AiSemanticExecutionTrace trace =
        new AiSemanticExecutionTrace(
            UUID.randomUUID(),
            TENANT_ID,
            PRINCIPAL_ID,
            "routing",
            "accepted",
            0.98,
            0.72,
            0.26,
            java.util.List.of("line\nquote\"tab\t"),
            null,
            null,
            null,
            1);

    recorder.recordSemanticOutcome(trace);

    verify(statement).param("matches", "[\"line\\nquote\\\"tab\\t\"]");
  }

  private static AiModelExecutionTrace modelTrace() {
    return new AiModelExecutionTrace(
        UUID.randomUUID(),
        "CHAT_COMPLETION",
        "local-ollama",
        "gemma-v1",
        "chat-v1",
        null,
        AiExecutionStatus.SUCCEEDED,
        12,
        10,
        4,
        14,
        new java.math.BigDecimal("0.0012"),
        "request",
        "response",
        null,
        null);
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
