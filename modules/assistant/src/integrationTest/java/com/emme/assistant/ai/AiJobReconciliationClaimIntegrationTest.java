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
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AiJobReconciliationClaimIntegrationTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final String CORE_SCHEMA = "emme" + '_' + "core";
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
  private RecordingAiJobMetrics metrics;

  @BeforeEach
  void setUpDurableJobState() throws Exception {
    dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbc = new JdbcTemplate(dataSource);
    adminJdbc = jdbc;
    jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + CORE_SCHEMA);
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
            new ClassPathResource("db/emme-core/releases/0.1.0/012-ai-job-state.sql")
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
    jdbc.update("DELETE FROM " + CORE_SCHEMA + ".ai_job_state");
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
    jdbc.execute("GRANT USAGE ON SCHEMA " + CORE_SCHEMA + " TO ai_job_runtime");
    jdbc.execute(
        "GRANT SELECT, INSERT, UPDATE ON TABLE " + CORE_SCHEMA + ".ai_job_state TO ai_job_runtime");
    assertThat(
            adminJdbc.queryForObject(
                "SELECT relforcerowsecurity FROM pg_class WHERE oid='"
                    + CORE_SCHEMA
                    + ".ai_job_state'::regclass",
                Boolean.class))
        .isTrue();
    adminJdbc =
        new JdbcTemplate(
            new CoreSchemaDataSource(
                new DriverManagerDataSource(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())));
    dataSource =
        new CoreSchemaDataSource(
            new DriverManagerDataSource(POSTGRES.getJdbcUrl(), RUNTIME_USERNAME, RUNTIME_PASSWORD));
    jdbc = new JdbcTemplate(dataSource);
    metrics = new RecordingAiJobMetrics();
    assertThat(jdbc.queryForObject("SELECT current_schema()", String.class)).isEqualTo(CORE_SCHEMA);
    store =
        new JdbcAiJobStatusStore(
            jdbc,
            3,
            new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
            metrics);
  }

  @Test
  void atomicallyClaimsOneQueuedJobWhenTwoReconciliationWorkersRace() throws Exception {
    AiExecutionContext context = context(TENANT_ID, "claim-race");
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
    runWithContext(context, () -> store.enqueue(request));
    assertThat(
            adminJdbc.queryForMap(
                "SELECT tenant_id, status, available_at FROM ai_job_state WHERE job_id = ?",
                request.jobId()))
        .containsEntry("tenant_id", TENANT_ID)
        .containsEntry("status", AiJobStatus.QUEUED.name());
    assertThat(
            adminJdbc.queryForObject(
                "SELECT available_at <= CURRENT_TIMESTAMP FROM ai_job_state WHERE job_id = ?",
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

    assertThat(metrics.queueLags).hasSize(1);
    assertThat(metrics.claimDurations).hasSize(2);

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

  @Test
  void claimsJobsWithEqualTimestampsInStableJobIdOrder() {
    AiExecutionContext context = context(TENANT_ID, "stable-job-order");
    UUID firstJobId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID secondJobId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    AiJobRequest secondRequest =
        new AiJobRequest(secondJobId, AiJobType.GRAPH_PROJECTION, "second", context);
    AiJobRequest firstRequest =
        new AiJobRequest(firstJobId, AiJobType.GRAPH_PROJECTION, "first", context);

    runWithContext(context, () -> store.enqueue(secondRequest));
    runWithContext(context, () -> store.enqueue(firstRequest));
    adminJdbc.update(
        "UPDATE ai_job_state SET created_at='2026-01-01T00:00:00Z'::timestamptz, available_at='2026-01-01T00:00:00Z'::timestamptz WHERE job_id IN (?, ?)",
        firstJobId,
        secondJobId);

    assertThat(withContext(context, () -> store.claimAvailable(2)))
        .extracting(AiJobRequest::jobId)
        .containsExactly(firstJobId, secondJobId);
  }

  @Test
  void progressesRetryBackoffAndMovesTheThirdFailureToDeadLetter() {
    AiExecutionContext context = context(TENANT_ID, "retry-progression");
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
    runWithContext(context, () -> store.enqueue(request));

    assertThat(withContext(context, () -> store.claimAndLoad(request.jobId(), context)))
        .isPresent();
    runWithContext(context, () -> store.fail(request.jobId(), "FIRST_FAILURE", context));
    assertThat(statusFor(context, request.jobId())).isEqualTo(AiJobStatus.RETRYING.name());
    assertThat(backoffSecondsFor(request.jobId())).isEqualTo(1.0);

    makeDue(request.jobId());
    assertThat(withContext(context, () -> store.claimAndLoad(request.jobId(), context)))
        .isPresent();
    runWithContext(context, () -> store.fail(request.jobId(), "SECOND_FAILURE", context));
    assertThat(statusFor(context, request.jobId())).isEqualTo(AiJobStatus.RETRYING.name());
    assertThat(backoffSecondsFor(request.jobId())).isEqualTo(2.0);

    makeDue(request.jobId());
    assertThat(withContext(context, () -> store.claimAndLoad(request.jobId(), context)))
        .isPresent();
    runWithContext(context, () -> store.fail(request.jobId(), "THIRD_FAILURE", context));

    assertThat(statusFor(context, request.jobId())).isEqualTo(AiJobStatus.DEAD_LETTER.name());
    assertThat(lastErrorFor(request.jobId())).isEqualTo("THIRD_FAILURE");
  }

  @Test
  void releasesARejectedClaimImmediatelyWithDurableNextAvailability() {
    AiExecutionContext context = context(TENANT_ID, "rejected-claim");
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
    runWithContext(context, () -> store.enqueue(request));
    assertThat(withContext(context, () -> store.claimAvailable(1))).hasSize(1);

    runWithContext(context, () -> store.defer(request.jobId(), context, Duration.ofSeconds(1)));

    assertThat(statusFor(context, request.jobId())).isEqualTo(AiJobStatus.RETRYING.name());
    assertThat(backoffSecondsFor(request.jobId())).isBetween(0.9, 1.1);
    assertThat(eligibleCountFor(context)).isZero();
  }

  @Test
  void rejectedDeliveryDoesNotConsumeExecutionAttemptsBeforeRetryAndDeadLetterPolicy() {
    AiExecutionContext context = context(TENANT_ID, "rejection-does-not-consume-attempts");
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
    runWithContext(context, () -> store.enqueue(request));

    for (int rejection = 0; rejection < 3; rejection++) {
      assertThat(withContext(context, () -> store.claimAndLoad(request.jobId(), context)))
          .isPresent();
      runWithContext(context, () -> store.defer(request.jobId(), context, Duration.ofSeconds(1)));
      makeDue(request.jobId());
    }

    assertThat(attemptsFor(request.jobId())).isZero();

    assertThat(withContext(context, () -> store.claimAndLoad(request.jobId(), context)))
        .isPresent();
    runWithContext(context, () -> store.fail(request.jobId(), "FIRST_REAL_FAILURE", context));
    assertThat(attemptsFor(request.jobId())).isEqualTo(1);
    assertThat(statusFor(context, request.jobId())).isEqualTo(AiJobStatus.RETRYING.name());

    makeDue(request.jobId());
    assertThat(withContext(context, () -> store.claimAndLoad(request.jobId(), context)))
        .isPresent();
    runWithContext(context, () -> store.fail(request.jobId(), "SECOND_REAL_FAILURE", context));
    assertThat(statusFor(context, request.jobId())).isEqualTo(AiJobStatus.RETRYING.name());

    makeDue(request.jobId());
    assertThat(withContext(context, () -> store.claimAndLoad(request.jobId(), context)))
        .isPresent();
    runWithContext(context, () -> store.fail(request.jobId(), "THIRD_REAL_FAILURE", context));
    assertThat(statusFor(context, request.jobId())).isEqualTo(AiJobStatus.DEAD_LETTER.name());
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
                  "SELECT status FROM ai_job_state WHERE job_id = ?", String.class, jobId);
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
                  "SELECT COUNT(*) FROM ai_job_state WHERE tenant_id = ?",
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
                  "SELECT COUNT(*) FROM ai_job_state WHERE tenant_id = ? AND tenant_id = current_tenant_id() AND available_at <= CURRENT_TIMESTAMP AND status IN ('QUEUED', 'RETRYING')",
                  Integer.class,
                  context.tenantId());
            });
  }

  private void makeDue(UUID jobId) {
    adminJdbc.update(
        "UPDATE ai_job_state SET available_at=CURRENT_TIMESTAMP WHERE job_id = ?", jobId);
  }

  private double backoffSecondsFor(UUID jobId) {
    return adminJdbc.queryForObject(
        "SELECT EXTRACT(EPOCH FROM (available_at - updated_at))::double precision FROM ai_job_state WHERE job_id = ?",
        Double.class,
        jobId);
  }

  private int attemptsFor(UUID jobId) {
    return adminJdbc.queryForObject(
        "SELECT attempts FROM ai_job_state WHERE job_id = ?", Integer.class, jobId);
  }

  private String lastErrorFor(UUID jobId) {
    return adminJdbc.queryForObject(
        "SELECT last_error FROM ai_job_state WHERE job_id = ?", String.class, jobId);
  }

  private static final class CoreSchemaDataSource extends AbstractDataSource {
    private final DataSource delegate;

    private CoreSchemaDataSource(DataSource delegate) {
      this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
      return inCoreSchema(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return inCoreSchema(delegate.getConnection(username, password));
    }

    private static Connection inCoreSchema(Connection connection) throws SQLException {
      try (var statement = connection.createStatement()) {
        statement.execute("SET search_path TO " + CORE_SCHEMA + ", public");
      }
      return connection;
    }
  }

  private static final class RecordingAiJobMetrics
      implements com.emme.assistant.ai.application.port.out.AiJobMetrics {
    private final List<Duration> queueLags = new CopyOnWriteArrayList<>();
    private final List<Duration> claimDurations = new CopyOnWriteArrayList<>();

    @Override
    public void recordQueueDepth(int depth) {}

    @Override
    public void recordQueueLag(Duration lag) {
      queueLags.add(lag);
    }

    @Override
    public void recordClaimDuration(Duration duration) {
      claimDurations.add(duration);
    }

    @Override
    public void recordClaim(String outcome) {}

    @Override
    public void recordFailure() {}

    @Override
    public void recordRetry() {}

    @Override
    public void recordDeadLetter() {}

    @Override
    public void recordTenantFairness() {}
  }
}
