package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.domain.job.AiJobStatus;
import com.emme.kernel.context.AiExecutionContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public final class JdbcAiJobStatusStore implements AiJobStatusStore {
  private final JdbcTemplate jdbc;
  private final int maxAttempts;

  @Autowired
  public JdbcAiJobStatusStore(
      JdbcTemplate jdbc, com.emme.assistant.ai.configuration.AiJobProperties properties) {
    this(jdbc, properties.maxAttempts());
  }

  public JdbcAiJobStatusStore(JdbcTemplate jdbc, int maxAttempts) {
    this.jdbc = jdbc;
    this.maxAttempts = maxAttempts;
  }

  @Override
  public void enqueue(AiJobRequest request) {
    jdbc.update(
        "INSERT INTO emme_core.ai_job_state(job_id,tenant_id,principal_id,roles,conversation_id,workflow_id,trace_id,idempotency_key,job_type,payload,status,available_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (job_id) DO NOTHING",
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
        AiJobStatus.QUEUED.name(),
        Timestamp.from(Instant.now()));
  }

  @Override
  public AiJobStatus claim(UUID jobId, AiExecutionContext context) {
    Integer exists =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM emme_core.ai_job_state WHERE job_id=?", Integer.class, jobId);
    if (exists == null || exists == 0) return AiJobStatus.NOT_FOUND;
    Integer tenant =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM emme_core.ai_job_state WHERE job_id=? AND tenant_id=?",
            Integer.class,
            jobId,
            context.tenantId());
    if (tenant == null || tenant == 0) return AiJobStatus.TENANT_MISMATCH;
    var rows =
        jdbc.queryForList(
            "UPDATE emme_core.ai_job_state SET status='CLAIMED', attempts=attempts+1, updated_at=CURRENT_TIMESTAMP WHERE job_id=? AND tenant_id=? AND status IN ('QUEUED','RETRYING') AND available_at<=CURRENT_TIMESTAMP RETURNING status",
            jobId,
            context.tenantId());
    return rows.isEmpty() ? AiJobStatus.NOT_AVAILABLE : AiJobStatus.CLAIMED;
  }

  @Override
  public void complete(UUID jobId, AiExecutionContext context) {
    jdbc.update(
        "UPDATE emme_core.ai_job_state SET status='COMPLETED', updated_at=CURRENT_TIMESTAMP WHERE job_id=? AND tenant_id=? AND status='CLAIMED'",
        jobId,
        context.tenantId());
  }

  @Override
  public void fail(UUID jobId, String errorCode, AiExecutionContext context) {
    jdbc.update(
        "UPDATE emme_core.ai_job_state SET status=CASE WHEN attempts >= ? THEN 'DEAD_LETTER' ELSE 'RETRYING' END, available_at=CURRENT_TIMESTAMP + (power(2, GREATEST(attempts-1,0)) * INTERVAL '1 second'), last_error=?, updated_at=CURRENT_TIMESTAMP WHERE job_id=? AND tenant_id=? AND status='CLAIMED'",
        maxAttempts,
        errorCode,
        jobId,
        context.tenantId());
  }

  @Override
  public java.util.List<AiJobRequest> findAvailable(int limit) {
    if (limit <= 0) return List.of();
    // JdbcTemplate cannot execute a multi-statement claim/recovery query portably. Recover stale
    // claims first, then read durable work under the tenant RLS/current-tenant predicate.
    jdbc.update(
        "UPDATE emme_core.ai_job_state SET status='RETRYING', updated_at=CURRENT_TIMESTAMP "
            + "WHERE status='CLAIMED' AND updated_at < CURRENT_TIMESTAMP - INTERVAL '5 minutes' "
            + "AND tenant_id = current_tenant_id()");
    return jdbc.query(
        "SELECT job_id, tenant_id, principal_id, roles, conversation_id, workflow_id, trace_id, idempotency_key, job_type, payload FROM emme_core.ai_job_state "
            + "WHERE tenant_id = current_tenant_id() AND available_at <= CURRENT_TIMESTAMP "
            + "AND status IN ('QUEUED','RETRYING') ORDER BY available_at, created_at "
            + "FOR UPDATE SKIP LOCKED LIMIT ?",
        (rs, rowNum) ->
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
                    rs.getString("idempotency_key"))),
        limit);
  }
}
