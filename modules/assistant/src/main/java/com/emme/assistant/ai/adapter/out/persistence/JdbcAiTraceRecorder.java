package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.trace.AiModelExecutionTrace;
import com.emme.assistant.ai.application.trace.AiToolCallTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.AiTraceRedactor;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import org.springframework.jdbc.core.simple.JdbcClient;

/** PostgreSQL adapter for tenant-scoped, redacted AI execution traces. */
public final class JdbcAiTraceRecorder implements AiTraceRecorder {

  private final JdbcClient jdbc;
  private final AiTraceRedactor redactor;

  public JdbcAiTraceRecorder(JdbcClient jdbc, AiTraceRedactor redactor) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    this.redactor = Objects.requireNonNull(redactor, "redactor must not be null");
  }

  @Override
  public void recordModelExecution(AiModelExecutionTrace trace) {
    Objects.requireNonNull(trace, "trace must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    jdbc.sql(
            """
            INSERT INTO ai_model_execution (
                id, tenant_id, principal_id, conversation_id, workflow_id, trace_id,
                execution_key, operation, provider_key, model_version, prompt_version,
                graph_version, status, request_payload, response_payload, latency_ms,
                input_tokens, output_tokens, total_tokens, estimated_cost,
                error_code, error_message
            )
            VALUES (
                :executionId, :tenantId, :principalId, :conversationId, :workflowId,
                :traceId, :executionKey, :operation, :providerKey, :modelVersion,
                :promptVersion, :graphVersion, :status,
                to_jsonb(CAST(:requestPayload AS text)),
                CASE WHEN :responsePayload IS NULL THEN NULL
                     ELSE to_jsonb(CAST(:responsePayload AS text)) END,
                :latencyMillis, :inputTokens, :outputTokens, :totalTokens,
                :estimatedCost, :errorCode, :errorMessage
            )
            ON CONFLICT (tenant_id, execution_key)
            DO UPDATE SET
                status = EXCLUDED.status,
                response_payload = EXCLUDED.response_payload,
                latency_ms = EXCLUDED.latency_ms,
                error_code = EXCLUDED.error_code,
                error_message = EXCLUDED.error_message,
                updated_at = CURRENT_TIMESTAMP,
                version = ai_model_execution.version + 1
            """)
        .param("executionId", trace.executionId())
        .param("tenantId", context.tenantId())
        .param("principalId", context.principalId())
        .param("conversationId", context.conversationId())
        .param("workflowId", context.workflowId())
        .param("traceId", context.traceId())
        .param("executionKey", trace.executionId().toString())
        .param("operation", trace.operation())
        .param("providerKey", trace.providerKey())
        .param("modelVersion", trace.modelVersion())
        .param("promptVersion", trace.promptVersion())
        .param("graphVersion", trace.graphVersion())
        .param("status", trace.status().name())
        .param("requestPayload", redactor.redact(trace.requestPayload()))
        .param("responsePayload", redactor.redact(trace.responsePayload()))
        .param("latencyMillis", trace.latencyMillis())
        .param("inputTokens", trace.inputTokens())
        .param("outputTokens", trace.outputTokens())
        .param("totalTokens", trace.totalTokens())
        .param("estimatedCost", trace.estimatedCost())
        .param("errorCode", trace.errorCode())
        .param("errorMessage", redactor.redact(trace.errorMessage()))
        .update();
  }

  @Override
  public void recordToolCall(AiToolCallTrace trace) {
    Objects.requireNonNull(trace, "trace must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    jdbc.sql(
            """
            INSERT INTO ai_tool_call (
                id, tenant_id, principal_id, conversation_id, workflow_id, trace_id,
                call_key, tool_key, risk_level, status, authorized, user_confirmed,
                staff_approved, arguments_payload, result_payload, latency_ms,
                error_code, error_message
            )
            VALUES (
                :callId, :tenantId, :principalId, :conversationId, :workflowId,
                :traceId, :callKey, :toolKey, :riskLevel, :status, :authorized,
                :userConfirmed, :staffApproved,
                to_jsonb(CAST(:argumentsPayload AS text)),
                CASE WHEN :resultPayload IS NULL THEN NULL
                     ELSE to_jsonb(CAST(:resultPayload AS text)) END,
                :latencyMillis, :errorCode, :errorMessage
            )
            ON CONFLICT (tenant_id, call_key)
            DO UPDATE SET
                status = EXCLUDED.status,
                authorized = EXCLUDED.authorized,
                result_payload = EXCLUDED.result_payload,
                latency_ms = EXCLUDED.latency_ms,
                error_code = EXCLUDED.error_code,
                error_message = EXCLUDED.error_message,
                updated_at = CURRENT_TIMESTAMP,
                version = ai_tool_call.version + 1
            """)
        .param("callId", trace.callId())
        .param("tenantId", context.tenantId())
        .param("principalId", context.principalId())
        .param("conversationId", context.conversationId())
        .param("workflowId", context.workflowId())
        .param("traceId", context.traceId())
        .param("callKey", trace.callId().toString())
        .param("toolKey", trace.toolKey())
        .param("riskLevel", trace.riskLevel())
        .param("status", trace.status().name())
        .param("authorized", trace.authorized())
        .param("userConfirmed", trace.userConfirmed())
        .param("staffApproved", trace.staffApproved())
        .param("argumentsPayload", redactor.redact(trace.argumentsPayload()))
        .param("resultPayload", redactor.redact(trace.resultPayload()))
        .param("latencyMillis", trace.latencyMillis())
        .param("errorCode", trace.errorCode())
        .param("errorMessage", redactor.redact(trace.errorMessage()))
        .update();
  }
}
