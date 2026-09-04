package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.TestApplication;
import com.emme.assistant.ai.adapter.out.persistence.JdbcQuoteWorkflowRepository;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
class QuoteWorkflowIdempotencyIntegrationTest {

  private static final UUID SETUP_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired private JdbcClient jdbc;
  @Autowired private DataSource dataSource;

  @BeforeEach
  void applyWorkflowMigration() {
    TenantContextHolder.withTenantOverride(
        SETUP_TENANT,
        () -> {
          jdbc.sql(
                  """
                  CREATE OR REPLACE FUNCTION current_tenant_id()
                  RETURNS UUID
                  LANGUAGE sql
                  STABLE
                  AS 'SELECT nullif(current_setting(''app.current_tenant_id'', true), '''')::UUID'
                  """)
              .update();
          new ResourceDatabasePopulator(
                  new ClassPathResource("db/emme-studio/releases/0.1.0/016-ai-quote-workflow.sql"))
              .execute(dataSource);
        });
  }

  @Test
  void returnsTheDurableWorkflowForARepeatedIdempotencyKeyAndRejectsAnotherPrincipal() {
    UUID tenantId = UUID.randomUUID();
    UUID principalId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID firstWorkflowId = UUID.randomUUID();
    UUID retryWorkflowId = UUID.randomUUID();
    UUID foreignWorkflowId = UUID.randomUUID();
    String idempotencyKey = "quote-integration-" + UUID.randomUUID();
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

  private static <T> T withContext(AiExecutionContext context, ThrowingSupplier<T> action) {
    return TenantContextHolder.withTenantOverride(
        context.tenantId(), () -> AiExecutionContextScope.call(context, action::get));
  }

  @FunctionalInterface
  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }
}
