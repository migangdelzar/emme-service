package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.job.AiJobStatus;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface AiJobStatusStore {
  void enqueue(com.emme.ai.contracts.job.AiJobRequest request);

  /** Atomically claims a due event job and returns its canonical durable request. */
  Optional<com.emme.ai.contracts.job.AiJobRequest> claimAndLoad(
      UUID jobId, AiExecutionContext eventContext);

  /** Reloads the canonical durable request for a job already claimed by reconciliation. */
  Optional<com.emme.ai.contracts.job.AiJobRequest> loadClaimed(
      UUID jobId, AiExecutionContext context);

  AiJobStatus claim(UUID jobId, AiExecutionContext context);

  void complete(UUID jobId, AiExecutionContext context);

  void fail(UUID jobId, String errorCode, AiExecutionContext context);

  /** Atomically releases a claimed job back to durable retryable work. */
  void defer(UUID jobId, AiExecutionContext context, Duration delay);

  /** Atomically claims and returns due jobs for the current backend tenant context. */
  java.util.List<com.emme.ai.contracts.job.AiJobRequest> claimAvailable(int limit);
}
