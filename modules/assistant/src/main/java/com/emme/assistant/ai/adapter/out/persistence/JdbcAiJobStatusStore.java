package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.domain.job.AiJobStatus;
import com.emme.kernel.context.AiExecutionContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public final class JdbcAiJobStatusStore implements AiJobStatusStore {
  private final JdbcTemplate jdbc;

  public JdbcAiJobStatusStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void enqueue(AiJobRequest request) {
    jdbc.update(
        "INSERT INTO emme_core.ai_job_state(job_id,tenant_id,job_type,payload,status,available_at) VALUES (?,?,?,?,?,?) ON CONFLICT (job_id) DO NOTHING",
        request.jobId(),
        request.context().tenantId(),
        request.type().name(),
        request.payload(),
        AiJobStatus.QUEUED.name(),
        Timestamp.from(Instant.now()));
  }

  @Override
  public AiJobStatus claim(UUID jobId, AiExecutionContext context) {
    var rows =
        jdbc.queryForList(
            "UPDATE emme_core.ai_job_state SET status='CLAIMED', attempts=attempts+1, updated_at=CURRENT_TIMESTAMP WHERE job_id=? AND tenant_id=? AND status IN ('QUEUED','RETRYING') AND available_at<=CURRENT_TIMESTAMP RETURNING status",
            jobId,
            context.tenantId());
    return rows.isEmpty() ? AiJobStatus.COMPLETED : AiJobStatus.CLAIMED;
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
        "UPDATE emme_core.ai_job_state SET status=CASE WHEN attempts >= 3 THEN 'DEAD_LETTER' ELSE 'RETRYING' END, available_at=CURRENT_TIMESTAMP + ((2 ^ GREATEST(attempts-1,0)) * INTERVAL '1 second'), last_error=?, updated_at=CURRENT_TIMESTAMP WHERE job_id=? AND tenant_id=? AND status='CLAIMED'",
        errorCode,
        jobId,
        context.tenantId());
  }
}
