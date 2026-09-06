package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowCheckpointRepository;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Persists payment resume state in the tenant-routed workflow run. */
@Component
public final class JdbcPaymentWorkflowCheckpointRepository
    implements PaymentWorkflowCheckpointRepository {

  private final JdbcClient jdbc;

  public JdbcPaymentWorkflowCheckpointRepository(@Qualifier("tenantJdbcClient") JdbcClient jdbc) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
  }

  @Override
  public boolean claimForResume(AiExecutionContext context) {
    return claimForResume(context, WorkflowStatus.WAITING_FOR_PAYMENT);
  }

  @Override
  public boolean claimForResume(AiExecutionContext context, WorkflowStatus expectedStatus) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(expectedStatus, "expectedStatus must not be null");
    requireTenant(context);
    return jdbc.sql(
                """
                UPDATE ai_workflow_run
                SET status = 'RUNNING', updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :workflowId
                  AND principal_id = :principalId
                  AND conversation_id = :conversationId
                  AND status = :expectedStatus
                """)
            .param("workflowId", context.workflowId())
            .param("principalId", context.principalId())
            .param("conversationId", context.conversationId())
            .param("expectedStatus", persistedStatus(expectedStatus))
            .update()
        == 1;
  }

  @Override
  public void record(AiExecutionContext context, WorkflowHandle handle) {
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(handle, "handle must not be null");
    requireTenant(context);
    if (!context.workflowId().equals(handle.workflowId())) {
      throw new SecurityException("Workflow checkpoint does not match execution context");
    }
    int updated =
        jdbc.sql(
                """
                UPDATE ai_workflow_run
                SET status = :status, updated_at = CURRENT_TIMESTAMP, version = version + 1
                WHERE id = :workflowId
                  AND principal_id = :principalId
                  AND conversation_id = :conversationId
                """)
            .param("status", persistedStatus(handle.status()))
            .param("workflowId", context.workflowId())
            .param("principalId", context.principalId())
            .param("conversationId", context.conversationId())
            .update();
    if (updated != 1) {
      throw new SecurityException("Workflow checkpoint is not owned by the execution context");
    }
  }

  private static void requireTenant(AiExecutionContext context) {
    if (!TenantContextHolder.requireCurrentTenantId().equals(context.tenantId())) {
      throw new SecurityException("Workflow checkpoint tenant does not match execution context");
    }
  }

  private static String persistedStatus(WorkflowStatus status) {
    return status == WorkflowStatus.CREATED ? "RECEIVED" : status.name();
  }
}
