package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.QuoteWorkflowRepository;
import com.emme.assistant.ai.application.security.AiStaffRolePolicy;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** PostgreSQL adapter for tenant-scoped quote workflow state and optimistic updates. */
@Component
public final class JdbcQuoteWorkflowRepository implements QuoteWorkflowRepository {

  private final JdbcClient jdbc;

  public JdbcQuoteWorkflowRepository(JdbcClient jdbc) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
  }

  @Override
  public Optional<QuoteWorkflow> findByIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("idempotencyKey must not be blank");
    }
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return jdbc
        .sql(
            """
            SELECT id, tenant_id, principal_id, conversation_id, status, idempotency_key, version
            FROM ai_workflow_run
            WHERE tenant_id = :tenantId
              AND idempotency_key = :idempotencyKey
            """)
        .param("tenantId", context.tenantId())
        .param("idempotencyKey", idempotencyKey)
        .query((resultSet, rowNumber) -> workflowFromRow(resultSet))
        .list()
        .stream()
        .findFirst();
  }

  @Override
  public Optional<QuoteWorkflow> findById(java.util.UUID workflowId) {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return jdbc
        .sql(
            """
            SELECT id, tenant_id, principal_id, conversation_id, status, idempotency_key, version
            FROM ai_workflow_run
            WHERE tenant_id = :tenantId
              AND id = :workflowId
            """)
        .param("tenantId", context.tenantId())
        .param("workflowId", workflowId)
        .query((resultSet, rowNumber) -> workflowFromRow(resultSet))
        .list()
        .stream()
        .findFirst();
  }

  @Override
  public QuoteWorkflow save(QuoteWorkflow workflow) {
    Objects.requireNonNull(workflow, "workflow must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    if (!context.tenantId().equals(workflow.tenantId())) {
      throw new IllegalArgumentException("tenantId does not match AI execution context");
    }
    if (!context.conversationId().equals(workflow.conversationId())) {
      throw new IllegalArgumentException("conversationId does not match AI execution context");
    }
    if (!context.principalId().equals(workflow.principalId()) && !isStaff(context)) {
      throw new IllegalStateException("principalId does not match AI execution context");
    }
    if (!context.workflowId().equals(workflow.id())) {
      throw new IllegalArgumentException("workflowId does not match AI execution context");
    }

    String sql = workflow.version() == 0 ? insertSql() : updateSql();
    var statement =
        jdbc.sql(sql)
            .param("tenantId", context.tenantId())
            .param("workflowId", workflow.id())
            .param("principalId", workflow.principalId())
            .param("conversationId", workflow.conversationId())
            .param("status", workflow.state().name())
            .param("idempotencyKey", workflow.idempotencyKey());
    if (workflow.version() > 0) {
      statement = statement.param("expectedVersion", workflow.version() - 1);
    }
    QuoteWorkflow saved =
        statement.query((resultSet, rowNumber) -> workflowFromRow(resultSet)).single();
    if (!context.conversationId().equals(saved.conversationId())) {
      throw new IllegalStateException("idempotency key belongs to another conversation");
    }
    if (!context.principalId().equals(saved.principalId()) && !isStaff(context)) {
      throw new IllegalStateException("idempotency key belongs to another principal");
    }
    return saved;
  }

  private static boolean isStaff(AiExecutionContext context) {
    return AiStaffRolePolicy.isStaff(context.roles());
  }

  private static String insertSql() {
    return """
    INSERT INTO ai_workflow_run (
        id, tenant_id, principal_id, conversation_id, workflow_type, status,
        graph_version, idempotency_key, state, version
    )
    VALUES (
        :workflowId, :tenantId, :principalId, :conversationId, 'DESIGN_QUOTE',
        :status, 'quote-v1', :idempotencyKey, CAST('{}' AS jsonb), 0
    )
    ON CONFLICT (tenant_id, idempotency_key) DO UPDATE
    SET id = ai_workflow_run.id
    RETURNING id, tenant_id, principal_id, conversation_id, status, idempotency_key, version
    """;
  }

  private static String updateSql() {
    return """
    UPDATE ai_workflow_run
    SET status = :status,
        updated_at = CURRENT_TIMESTAMP,
        version = :expectedVersion + 1
    WHERE id = :workflowId
      AND tenant_id = :tenantId
      AND version = :expectedVersion
    RETURNING id, tenant_id, principal_id, conversation_id, status, idempotency_key, version
    """;
  }

  private static QuoteWorkflow workflowFromRow(java.sql.ResultSet resultSet)
      throws java.sql.SQLException {
    return new QuoteWorkflow(
        resultSet.getObject("id", java.util.UUID.class),
        resultSet.getObject("tenant_id", java.util.UUID.class),
        resultSet.getObject("principal_id", java.util.UUID.class),
        resultSet.getObject("conversation_id", java.util.UUID.class),
        QuoteWorkflowState.valueOf(resultSet.getString("status")),
        resultSet.getString("idempotency_key"),
        resultSet.getLong("version"));
  }
}
