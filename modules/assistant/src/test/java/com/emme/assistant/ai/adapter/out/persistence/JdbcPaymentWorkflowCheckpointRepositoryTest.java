package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class JdbcPaymentWorkflowCheckpointRepositoryTest {

  @Test
  void isOptInWithTheLangGraphWorkflowBoundary() {
    assertThat(JdbcPaymentWorkflowCheckpointRepository.class)
        .hasAnnotation(ConditionalOnProperty.class);
    assertThat(
            JdbcPaymentWorkflowCheckpointRepository.class
                .getAnnotation(ConditionalOnProperty.class)
                .prefix())
        .isEqualTo("app.ai.langgraph");
  }

  @Test
  void claimsAndRecordsUsingTheTenantRoutedWorkflowRunWithoutRepeatingTenantPredicates() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcPaymentWorkflowCheckpointRepository repository =
        new JdbcPaymentWorkflowCheckpointRepository(jdbc);
    AiExecutionContext context = context();

    boolean claimed =
        TenantContextHolder.withTenantOverride(
            context.tenantId(), () -> repository.claimForResume(context));
    TenantContextHolder.withTenantOverride(
        context.tenantId(),
        () ->
            repository.record(
                context, new WorkflowHandle(context.workflowId(), WorkflowStatus.SUCCEEDED, 1)));

    assertThat(claimed).isTrue();
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc, org.mockito.Mockito.atLeast(2)).sql(sql.capture());
    assertThat(sql.getAllValues())
        .allSatisfy(
            statementSql -> {
              assertThat(statementSql).contains("ai_workflow_run");
              assertThat(statementSql).doesNotContain("tenant_id =");
            });
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("CLIENT"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-1",
        "workflow-1");
  }
}
