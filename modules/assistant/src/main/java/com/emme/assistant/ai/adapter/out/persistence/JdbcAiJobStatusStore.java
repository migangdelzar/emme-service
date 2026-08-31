package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.assistant.ai.application.port.out.AiJobMetrics;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.application.port.out.NoopAiJobMetrics;
import com.emme.assistant.ai.domain.job.AiJobStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

@Component
public final class JdbcAiJobStatusStore implements AiJobStatusStore {
  private final JdbcTemplate jdbc;
  private final int maxAttempts;
  private final TransactionOperations transactions;
  private final AiJobMetrics metrics;

  @Autowired
  public JdbcAiJobStatusStore(
      JdbcTemplate jdbc,
      com.emme.assistant.ai.configuration.AiJobProperties properties,
      TransactionOperations transactions,
      AiJobMetrics metrics) {
    this(jdbc, properties.maxAttempts(), transactions, metrics);
  }

  public JdbcAiJobStatusStore(
      JdbcTemplate jdbc, int maxAttempts, TransactionOperations transactions) {
    this(jdbc, maxAttempts, transactions, NoopAiJobMetrics.INSTANCE);
  }

  public JdbcAiJobStatusStore(
      JdbcTemplate jdbc,
      int maxAttempts,
      TransactionOperations transactions,
      AiJobMetrics metrics) {
    this.jdbc = jdbc;
    this.maxAttempts = maxAttempts;
    this.transactions = Objects.requireNonNull(transactions, "transactions must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
  }

  @Override
  public void enqueue(AiJobRequest request) {
    inTenantTransaction(
        request.context(),
        () -> {
          jdbc.update(
              "INSERT INTO emme_core.ai_job_state(job_id,tenant_id,principal_id,roles,conversation_id,workflow_id,trace_id,idempotency_key,job_type,payload,status) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (job_id) DO NOTHING",
              request.jobId(),
              request.context().tenantId(),
              request.context().principalId(),
              String.join(",", request.context().roles()),
              request.context().conversationId(),
              request.context().workflowId(),
              request.context().traceId(),
              request.context().idempotencyKey(),
              request.type().name(),
              request.payload(),
              AiJobStatus.QUEUED.name());
          return null;
        });
  }

  @Override
  public Optional<AiJobRequest> claimAndLoad(UUID jobId, AiExecutionContext context) {
    Optional<AiJobRequest> claimed =
        inTenantTransaction(
            context,
            () ->
                jdbc
                    .query(
                        "UPDATE emme_core.ai_job_state SET status='CLAIMED', attempts=attempts+1, updated_at=CURRENT_TIMESTAMP WHERE job_id=? AND tenant_id=? AND tenant_id=current_tenant_id() AND status IN ('QUEUED','RETRYING') AND available_at<=CURRENT_TIMESTAMP RETURNING job_id, tenant_id, principal_id, roles, conversation_id, workflow_id, trace_id, idempotency_key, job_type, payload",
                        jobRequestRowMapper(),
                        jobId,
                        context.tenantId())
                    .stream()
                    .findFirst());
    metrics.recordClaim(claimed.isPresent() ? "claimed" : "not_available");
    return claimed;
  }

  @Override
  public Optional<AiJobRequest> loadClaimed(UUID jobId, AiExecutionContext context) {
    return inTenantTransaction(
        context,
        () ->
            jdbc
                .query(
                    "SELECT job_id, tenant_id, principal_id, roles, conversation_id, workflow_id, trace_id, idempotency_key, job_type, payload FROM emme_core.ai_job_state WHERE job_id=? AND tenant_id=? AND tenant_id=current_tenant_id() AND status='CLAIMED'",
                    jobRequestRowMapper(),
                    jobId,
                    context.tenantId())
                .stream()
                .findFirst());
  }

  @Override
  public AiJobStatus claim(UUID jobId, AiExecutionContext context) {
    return claimAndLoad(jobId, context).isPresent()
        ? AiJobStatus.CLAIMED
        : AiJobStatus.NOT_AVAILABLE;
  }

  @Override
  public void complete(UUID jobId, AiExecutionContext context) {
    inTenantTransaction(
        context,
        () -> {
          jdbc.update(
              "UPDATE emme_core.ai_job_state SET status='COMPLETED', updated_at=CURRENT_TIMESTAMP WHERE job_id=? AND tenant_id=? AND tenant_id=current_tenant_id() AND status='CLAIMED'",
              jobId,
              context.tenantId());
          return null;
        });
  }

  @Override
  public void fail(UUID jobId, String errorCode, AiExecutionContext context) {
    inTenantTransaction(
        context,
        () -> {
          List<String> statuses =
              jdbc.query(
                  "UPDATE emme_core.ai_job_state SET status=CASE WHEN attempts >= ? THEN 'DEAD_LETTER' ELSE 'RETRYING' END, available_at=CURRENT_TIMESTAMP + (power(2, GREATEST(attempts-1,0)) * INTERVAL '1 second'), last_error=?, updated_at=CURRENT_TIMESTAMP WHERE job_id=? AND tenant_id=? AND tenant_id=current_tenant_id() AND status='CLAIMED' RETURNING status",
                  (rs, rowNum) -> rs.getString("status"),
                  maxAttempts,
                  errorCode,
                  jobId,
                  context.tenantId());
          if (!statuses.isEmpty()) {
            metrics.recordFailure();
            if ("DEAD_LETTER".equals(statuses.getFirst())) metrics.recordDeadLetter();
            else metrics.recordRetry();
          }
          return null;
        });
  }

  @Override
  public List<AiJobRequest> claimAvailable(int limit) {
    if (limit <= 0) return List.of();
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return inTenantTransaction(
        context,
        () -> {
          int recoveredClaims =
              jdbc.update(
                  "UPDATE emme_core.ai_job_state SET status='RETRYING', available_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE status='CLAIMED' AND updated_at < CURRENT_TIMESTAMP - INTERVAL '5 minutes' AND tenant_id=? AND tenant_id=current_tenant_id()",
                  context.tenantId());
          for (int i = 0; i < recoveredClaims; i++) metrics.recordRetry();
          List<AiJobRequest> claimed =
              jdbc.query(
                  "WITH candidates AS (SELECT job_id FROM emme_core.ai_job_state WHERE tenant_id=? AND tenant_id=current_tenant_id() AND available_at<=CURRENT_TIMESTAMP AND status IN ('QUEUED','RETRYING') ORDER BY available_at, created_at FOR UPDATE SKIP LOCKED LIMIT ?) UPDATE emme_core.ai_job_state job SET status='CLAIMED', attempts=job.attempts+1, updated_at=CURRENT_TIMESTAMP FROM candidates WHERE job.job_id=candidates.job_id RETURNING job.job_id, job.tenant_id, job.principal_id, job.roles, job.conversation_id, job.workflow_id, job.trace_id, job.idempotency_key, job.job_type, job.payload",
                  jobRequestRowMapper(),
                  context.tenantId(),
                  limit);
          for (int i = 0; i < claimed.size(); i++) metrics.recordClaim("claimed");
          return claimed;
        });
  }

  private <T> T inTenantTransaction(
      AiExecutionContext context, java.util.function.Supplier<T> action) {
    Objects.requireNonNull(context, "context must not be null");
    return transactions.execute(
        status -> {
          jdbc.queryForObject(
              "SELECT set_config('app.current_tenant_id', ?, true)",
              String.class,
              context.tenantId().toString());
          return action.get();
        });
  }

  private static org.springframework.jdbc.core.RowMapper<AiJobRequest> jobRequestRowMapper() {
    return (rs, rowNum) ->
        new AiJobRequest(
            rs.getObject("job_id", UUID.class),
            com.emme.ai.contracts.job.AiJobType.valueOf(rs.getString("job_type")),
            rs.getString("payload"),
            new AiExecutionContext(
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("principal_id", UUID.class),
                java.util.Set.of(rs.getString("roles").split(",")),
                rs.getObject("conversation_id", UUID.class),
                rs.getObject("workflow_id", UUID.class),
                rs.getString("trace_id"),
                rs.getString("idempotency_key")));
  }
}
