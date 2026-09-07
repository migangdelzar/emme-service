package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.TestApplication;
import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.ai.contracts.payment.PaymentWorkflowStatus;
import com.emme.ai.contracts.workflow.PaymentWorkflow;
import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.assistant.ai.adapter.in.messaging.PaymentWorkflowEventListener;
import com.emme.assistant.ai.adapter.out.persistence.JdbcPaymentWorkflowCheckpointRepository;
import com.emme.assistant.ai.adapter.out.persistence.JdbcPaymentWorkflowExecutionContextRepository;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
class PaymentWorkflowDuplicateDeliveryIntegrationTest {

  @Autowired
  @Qualifier("tenantJdbcClient")
  private JdbcClient jdbc;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Test
  void concurrentlyDeliveredPaymentEventsResumeTheWorkflowOnlyOnce() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID principalId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();
    provisionTenant(tenantId);
    createWorkflowRun(tenantId, principalId, conversationId, workflowId);

    AtomicInteger resumes = new AtomicInteger();
    PaymentWorkflow workflow =
        (event, context) -> {
          resumes.incrementAndGet();
          return new WorkflowHandle(workflowId, WorkflowStatus.SUCCEEDED, 1);
        };
    PaymentWorkflowEventListener listener =
        new PaymentWorkflowEventListener(
            workflow,
            new JdbcPaymentWorkflowExecutionContextRepository(jdbc),
            new JdbcPaymentWorkflowCheckpointRepository(jdbc));
    PaymentWorkflowEvent event =
        new PaymentWorkflowEvent(
            tenantId,
            workflowId,
            "mock",
            "duplicate-event",
            "provider-reference",
            PaymentWorkflowStatus.CAPTURED);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<?> first = executor.submit(() -> deliver(listener, event, start));
      Future<?> second = executor.submit(() -> deliver(listener, event, start));
      start.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }

    assertThat(resumes).hasValue(1);
    String status =
        TenantContextHolder.withTenantOverride(
            tenantId,
            () ->
                jdbc.sql("SELECT status FROM ai_workflow_run WHERE id = :workflowId")
                    .param("workflowId", workflowId)
                    .query(String.class)
                    .single());
    assertThat(status).isEqualTo("SUCCEEDED");
  }

  private static void deliver(
      PaymentWorkflowEventListener listener, PaymentWorkflowEvent event, CountDownLatch start) {
    await(start);
    listener.onPaymentWorkflow(event);
  }

  private void createWorkflowRun(
      UUID tenantId, UUID principalId, UUID conversationId, UUID workflowId) {
    TenantContextHolder.withTenantOverride(
        tenantId,
        () -> {
          jdbc.sql(
                  "INSERT INTO conversation (id, tenant_id, participant_id, channel) "
                      + "VALUES (:id, :tenantId, :participantId, 'WEB_CHAT')")
              .param("id", conversationId)
              .param("tenantId", tenantId)
              .param("participantId", principalId)
              .update();
          jdbc.sql(
                  """
                  INSERT INTO ai_workflow_run (
                      id, tenant_id, principal_id, conversation_id, workflow_type,
                      status, graph_version, idempotency_key, state
                  ) VALUES (
                      :workflowId, :tenantId, :principalId, :conversationId,
                      'APPOINTMENT_PAYMENT', 'WAITING_FOR_PAYMENT', 'payment-v1',
                      :idempotencyKey, CAST('{}' AS jsonb)
                  )
                  """)
              .param("workflowId", workflowId)
              .param("tenantId", tenantId)
              .param("principalId", principalId)
              .param("conversationId", conversationId)
              .param("idempotencyKey", "payment-duplicate-" + workflowId)
              .update();
        });
  }

  private void provisionTenant(UUID tenantId) {
    String slug = "payment-duplicate-" + tenantId.toString().replace('-', 'a');
    String schemaName = TenantSchemaName.fromSlug(slug);
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
    provisioningRepository.requestProvisioning(tenantId, slug, schemaName);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(schemaName);
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting to deliver duplicate payment events");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while delivering duplicate payment events", interrupted);
    }
  }
}
