package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.AiToolIdempotencyStore;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;

/** PostgreSQL-backed idempotency store for tenant-scoped mutation tool commands. */
public final class JdbcAiToolIdempotencyStore implements AiToolIdempotencyStore {

  private final JdbcClient jdbc;
  private final ObjectMapper objectMapper;
  private final Duration claimLease;

  public JdbcAiToolIdempotencyStore(JdbcClient jdbc, ObjectMapper objectMapper) {
    this(jdbc, objectMapper, Duration.ofMinutes(15));
  }

  public JdbcAiToolIdempotencyStore(
      JdbcClient jdbc, ObjectMapper objectMapper, Duration claimLease) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.claimLease = requireClaimLease(claimLease);
  }

  @Override
  public Optional<AiToolResult> find(String operationKey) {
    requireText(operationKey, "operationKey");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return jdbc
        .sql(
            """
            SELECT result_payload::text AS result_payload
            FROM ai_tool_idempotency
            WHERE tenant_id = :tenantId
              AND principal_id = :principalId
              AND operation_key = :operationKey
              AND status = 'SUCCEEDED'
            """)
        .param("tenantId", context.tenantId())
        .param("principalId", context.principalId())
        .param("operationKey", operationKey)
        .query((resultSet, rowNumber) -> deserializeResult(resultSet.getString("result_payload")))
        .list()
        .stream()
        .findFirst();
  }

  @Override
  public boolean claim(String operationKey, String toolKey) {
    requireText(operationKey, "operationKey");
    requireText(toolKey, "toolKey");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return jdbc.sql(
                """
                INSERT INTO ai_tool_idempotency (
                    tenant_id, principal_id, operation_key, tool_key, status, result_payload,
                    lease_expires_at
                )
                VALUES (
                    :tenantId, :principalId, :operationKey, :toolKey, 'IN_PROGRESS', NULL,
                    CURRENT_TIMESTAMP + (:claimLeaseSeconds * INTERVAL '1 second')
                )
                ON CONFLICT (tenant_id, principal_id, operation_key) DO UPDATE SET
                    tool_key = EXCLUDED.tool_key,
                    status = 'IN_PROGRESS',
                    result_payload = NULL,
                    lease_expires_at = EXCLUDED.lease_expires_at,
                    updated_at = CURRENT_TIMESTAMP,
                    version = ai_tool_idempotency.version + 1
                WHERE ai_tool_idempotency.status = 'IN_PROGRESS'
                  AND ai_tool_idempotency.lease_expires_at <= CURRENT_TIMESTAMP
                """)
            .param("tenantId", context.tenantId())
            .param("principalId", context.principalId())
            .param("operationKey", operationKey)
            .param("toolKey", toolKey)
            .param("claimLeaseSeconds", claimLease.toSeconds())
            .update()
        > 0;
  }

  @Override
  public void complete(String operationKey, AiToolResult result) {
    requireText(operationKey, "operationKey");
    Objects.requireNonNull(result, "result must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    int updated =
        jdbc.sql(
                """
                UPDATE ai_tool_idempotency
                SET status = 'SUCCEEDED',
                    result_payload = CAST(:resultPayload AS jsonb),
                    lease_expires_at = NULL,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE tenant_id = :tenantId
                  AND principal_id = :principalId
                  AND operation_key = :operationKey
                  AND status = 'IN_PROGRESS'
                """)
            .param("tenantId", context.tenantId())
            .param("principalId", context.principalId())
            .param("operationKey", operationKey)
            .param("resultPayload", serializeResult(result))
            .update();
    if (updated == 0) {
      throw new IllegalStateException(
          "AI tool idempotency claim is no longer active: " + operationKey);
    }
  }

  @Override
  public void release(String operationKey) {
    requireText(operationKey, "operationKey");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    jdbc.sql(
            """
            DELETE FROM ai_tool_idempotency
            WHERE tenant_id = :tenantId
              AND principal_id = :principalId
              AND operation_key = :operationKey
              AND status = 'IN_PROGRESS'
            """)
        .param("tenantId", context.tenantId())
        .param("principalId", context.principalId())
        .param("operationKey", operationKey)
        .update();
  }

  private AiToolResult deserializeResult(String payload) {
    if (payload == null || payload.isBlank()) {
      throw new IllegalStateException("Completed AI tool idempotency result is missing");
    }
    try {
      return objectMapper.readValue(payload, AiToolResult.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Unable to deserialize AI tool idempotency result", exception);
    }
  }

  private String serializeResult(AiToolResult result) {
    try {
      return objectMapper.writeValueAsString(result);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize AI tool idempotency result", exception);
    }
  }

  private static void requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }

  private static Duration requireClaimLease(Duration claimLease) {
    Objects.requireNonNull(claimLease, "claimLease must not be null");
    if (claimLease.isZero() || claimLease.isNegative()) {
      throw new IllegalArgumentException("claimLease must be positive");
    }
    if (claimLease.compareTo(Duration.ofDays(1)) > 0) {
      throw new IllegalArgumentException("claimLease must not exceed one day");
    }
    return claimLease;
  }
}
