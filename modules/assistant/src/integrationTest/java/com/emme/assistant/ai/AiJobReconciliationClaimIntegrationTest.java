package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.ai.contracts.job.AiJobType;
import com.emme.assistant.ai.adapter.out.persistence.JdbcAiJobStatusStore;
import com.emme.assistant.ai.domain.job.AiJobStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextBridge;
import com.emme.kernel.context.AiExecutionContextScope;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AiJobReconciliationClaimIntegrationTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final String RUNTIME_USERNAME = "ai_job_runtime";
  private static final String RUNTIME_PASSWORD = "ai_job_runtime";

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("emme_test")
          .withUsername("emme")
          .withPassword("emme");

  private JdbcTemplate jdbc;
  private JdbcTemplate adminJdbc;
  private DataSource dataSource;

  private JdbcAiJobStatusStore store;

  @BeforeEach
  void setUpDurableJobState() throws Exception {
    dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbc = new JdbcTemplate(dataSource);
    adminJdbc = jdbc;
    jdbc.execute("CREATE SCHEMA IF NOT EXISTS emme_core");
    jdbc.execute(
        """
        CREATE OR REPLACE FUNCTION current_tenant_id()
        RETURNS UUID
        LANGUAGE sql
        STABLE
        AS 'SELECT nullif(current_setting(''app.current_tenant_id'', true), '''')::UUID'
        """);
    String migration =
        new String(
            new ClassPathResource("db/emme-studio/releases/0.1.0/028-ai-job-state.sql")
                .getInputStream()
                .readAllBytes(),
            StandardCharsets.UTF_8);
    jdbc.execute(
        (ConnectionCallback<Void>)
            connection -> {
              try (var statement = connection.createStatement()) {
                statement.execute(migration);
              }
              return null;
            });
    jdbc.update("DELETE FROM emme_core.ai_job_state");
    jdbc.execute(
        """
        DO $$
        BEGIN
          IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'ai_job_runtime') THEN
            CREATE ROLE ai_job_runtime LOGIN PASSWORD 'ai_job_runtime' NOSUPERUSER NOCREATEDB NOCREATEROLE;
          END IF;
        END
        $$
        """);
    jdbc.execute("GRANT USAGE ON SCHEMA emme_core TO ai_job_runtime");
    jdbc.execute("GRANT SELECT, INSERT, UPDATE ON TABLE emme_core.ai_job_state TO ai_job_runtime");
    jdbc.execute("ALTER TABLE emme_core.ai_job_state FORCE ROW LEVEL SECURITY");
    dataSource =
        new DriverManagerDataSource(POSTGRES.getJdbcUrl(), RUNTIME_USERNAME, RUNTIME_PASSWORD);
    jdbc = new JdbcTemplate(dataSource);
    store =
        new JdbcAiJobStatusStore(
            jdbc, 3, new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
  }

  @Test
  void atomicallyClaimsOneQueuedJobWhenTwoReconciliationWorkersRace() throws Exception {
    AiExecutionContext context = context(TENANT_ID, "claim-race");
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
    runWithContext(context, () -> store.enqueue(request));
    assertThat(
            adminJdbc.queryForMap(
                "SELECT tenant_id, status, available_at FROM emme_core.ai_job_state WHERE job_id = ?",
                request.jobId()))
        .containsEntry("tenant_id", TENANT_ID)
        .containsEntry("status", AiJobStatus.QUEUED.name());
    assertThat(
            adminJdbc.queryForObject(
                "SELECT available_at <= CURRENT_TIMESTAMP FROM emme_core.ai_job_state WHERE job_id = ?",
                Boolean.class,
                request.jobId()))
        .isTrue();
    assertThat(visibleCountFor(context)).isEqualTo(1);
    assertThat(eligibleCountFor(context)).isEqualTo(1);

    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<List<AiJobRequest>> claim =
          () -> withContext(context, () -> store.claimAvailable(1));
      Future<List<AiJobRequest>> first = executor.submit(claim);
      Future<List<AiJobRequest>> second = executor.submit(claim);

      List<AiJobRequest> firstClaim = first.get();
      List<AiJobRequest> secondClaim = second.get();
      List<AiJobRequest> combinedClaims = new ArrayList<>(firstClaim);
      combinedClaims.addAll(secondClaim);
      assertThat(combinedClaims).extracting(AiJobRequest::jobId).containsExactly(request.jobId());
    }

    assertThat(statusFor(context, request.jobId())).isEqualTo(AiJobStatus.CLAIMED.name());
  }

  @Test
  void claimsOnlyRowsForTheTenantBoundToTheBackendExecutionContext() {
    UUID otherTenant = UUID.randomUUID();
    AiExecutionContext firstContext = context(TENANT_ID, "tenant-a");
    AiExecutionContext secondContext = context(otherTenant, "tenant-b");
    AiJobRequest first =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "first", firstContext);
    AiJobRequest second =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "second", secondContext);

    runWithContext(firstContext, () -> store.enqueue(first));
    runWithContext(secondContext, () -> store.enqueue(second));

    List<AiJobRequest> firstClaim = withContext(firstContext, () -> store.claimAvailable(10));
    List<AiJobRequest> secondClaim = withContext(secondContext, () -> store.claimAvailable(10));
    assertThat(firstClaim).extracting(AiJobRequest::jobId).containsExactly(first.jobId());
    assertThat(secondClaim).extracting(AiJobRequest::jobId).containsExactly(second.jobId());
  }

  private static AiExecutionContext context(UUID tenantId, String key) {
    return new AiExecutionContext(
        tenantId,
        UUID.randomUUID(),
        Set.of("SYSTEM"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-" + key,
        "idempotency-" + key);
  }

  private static <T> T withContext(AiExecutionContext context, Callable<T> action) {
    return AiExecutionContextScope.call(
        context, () -> AiExecutionContextBridge.callCurrent(() -> action.call()));
  }

  private static void runWithContext(AiExecutionContext context, Runnable action) {
    AiExecutionContextScope.run(
        context, () -> AiExecutionContextBridge.runCurrent(() -> action.run()));
  }

  private String statusFor(AiExecutionContext context, UUID jobId) {
    return new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        .execute(
            status -> {
              jdbc.queryForObject(
                  "SELECT set_config('app.current_tenant_id', ?, true)",
                  String.class,
                  context.tenantId().toString());
              return jdbc.queryForObject(
                  "SELECT status FROM emme_core.ai_job_state WHERE job_id = ?",
                  String.class,
                  jobId);
            });
  }

  private int visibleCountFor(AiExecutionContext context) {
    return new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        .execute(
            status -> {
              jdbc.queryForObject(
                  "SELECT set_config('app.current_tenant_id', ?, true)",
                  String.class,
                  context.tenantId().toString());
              return jdbc.queryForObject(
                  "SELECT COUNT(*) FROM emme_core.ai_job_state WHERE tenant_id = ?",
                  Integer.class,
                  context.tenantId());
            });
  }

  private int eligibleCountFor(AiExecutionContext context) {
    return new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        .execute(
            status -> {
              jdbc.queryForObject(
                  "SELECT set_config('app.current_tenant_id', ?, true)",
                  String.class,
                  context.tenantId().toString());
              return jdbc.queryForObject(
                  "SELECT COUNT(*) FROM emme_core.ai_job_state WHERE tenant_id = ? AND tenant_id = current_tenant_id() AND available_at <= CURRENT_TIMESTAMP AND status IN ('QUEUED', 'RETRYING')",
                  Integer.class,
                  context.tenantId());
            });
  }
}
