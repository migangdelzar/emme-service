package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.PaymentWorkflowExecutionContextRepository;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Loads payment workflow ownership from the tenant-routed workflow state. */
@Component
public final class JdbcPaymentWorkflowExecutionContextRepository
    implements PaymentWorkflowExecutionContextRepository {

  private final JdbcClient jdbc;

  public JdbcPaymentWorkflowExecutionContextRepository(
      @Qualifier("tenantJdbcClient") JdbcClient jdbc) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
  }

  @Override
  public Optional<WorkflowExecutionContext> findByWorkflowId(UUID workflowId) {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    TenantContextHolder.requireCurrentTenantId();
    return jdbc
        .sql(
            """
            SELECT principal_id, conversation_id, idempotency_key
            FROM ai_workflow_run
            WHERE id = :workflowId
            """)
        .param("workflowId", workflowId)
        .query(
            (resultSet, rowNumber) ->
                new WorkflowExecutionContext(
                    resultSet.getObject("principal_id", UUID.class),
                    resultSet.getObject("conversation_id", UUID.class),
                    resultSet.getString("idempotency_key")))
        .list()
        .stream()
        .findFirst();
  }
}
