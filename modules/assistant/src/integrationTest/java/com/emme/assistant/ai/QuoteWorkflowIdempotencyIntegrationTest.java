package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.TestApplication;
import com.emme.assistant.ai.adapter.out.persistence.JdbcQuoteWorkflowRepository;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
class QuoteWorkflowIdempotencyIntegrationTest {

  @Autowired
  @Qualifier("tenantJdbcClient")
  private JdbcClient jdbc;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Test
  void returnsTheDurableWorkflowForARepeatedIdempotencyKeyAndRejectsAnotherPrincipal() {
    UUID tenantId = UUID.randomUUID();
    UUID principalId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID firstWorkflowId = UUID.randomUUID();
    UUID retryWorkflowId = UUID.randomUUID();
    UUID foreignWorkflowId = UUID.randomUUID();
    String idempotencyKey = "quote-integration-" + UUID.randomUUID();
    provisionTenant(tenantId);
    createConversation(tenantId, conversationId, principalId);
    JdbcQuoteWorkflowRepository repository = new JdbcQuoteWorkflowRepository(jdbc);

    AiExecutionContext firstContext =
        context(
            tenantId, principalId, conversationId, firstWorkflowId, idempotencyKey, "ROLE_CLIENT");
    QuoteWorkflow first =
        withContext(
            firstContext,
            () ->
                repository.save(
                    QuoteWorkflow.received(
                        firstWorkflowId, tenantId, principalId, conversationId, idempotencyKey)));

    AiExecutionContext retryContext =
        context(
            tenantId, principalId, conversationId, retryWorkflowId, idempotencyKey, "ROLE_CLIENT");
    QuoteWorkflow retry =
        withContext(
            retryContext,
            () ->
                repository.save(
                    QuoteWorkflow.received(
                        retryWorkflowId, tenantId, principalId, conversationId, idempotencyKey)));

    assertThat(retry.id()).isEqualTo(first.id());
    assertThat(retry.version()).isEqualTo(first.version());

    AiExecutionContext foreignContext =
        context(
            tenantId,
            UUID.randomUUID(),
            conversationId,
            foreignWorkflowId,
            idempotencyKey,
            "ROLE_CLIENT");
    assertThatThrownBy(
            () ->
                withContext(
                    foreignContext,
                    () ->
                        repository.save(
                            QuoteWorkflow.received(
                                foreignWorkflowId,
                                tenantId,
                                foreignContext.principalId(),
                                conversationId,
                                idempotencyKey))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("idempotency key belongs to another principal");
  }

  private static AiExecutionContext context(
      UUID tenantId,
      UUID principalId,
      UUID conversationId,
      UUID workflowId,
      String idempotencyKey,
      String role) {
    return new AiExecutionContext(
        tenantId,
        principalId,
        Set.of(role),
        conversationId,
        workflowId,
        "trace-" + UUID.randomUUID(),
        idempotencyKey);
  }

  private void provisionTenant(UUID tenantId) {
    String slug = "quote-" + tenantId.toString().replace('-', 'a');
    String schemaName = TenantSchemaName.fromSlug(slug);
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
    provisioningRepository.requestProvisioning(tenantId, slug, schemaName);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(schemaName);
  }

  private void createConversation(UUID tenantId, UUID conversationId, UUID participantId) {
    TenantContextHolder.withTenantOverride(
        tenantId,
        () ->
            jdbc.sql(
                    "INSERT INTO conversation (id, tenant_id, participant_id, channel) "
                        + "VALUES (:id, :tenantId, :participantId, 'WEB_CHAT')")
                .param("id", conversationId)
                .param("tenantId", tenantId)
                .param("participantId", participantId)
                .update());
  }

  private static <T> T withContext(AiExecutionContext context, ThrowingSupplier<T> action) {
    return TenantContextHolder.withTenantOverride(
        context.tenantId(), () -> AiExecutionContextScope.call(context, action::get));
  }

  @FunctionalInterface
  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }
}
