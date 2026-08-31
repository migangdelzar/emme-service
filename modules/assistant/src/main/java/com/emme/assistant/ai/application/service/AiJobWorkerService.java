package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.domain.job.AiJobStatus;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Duration;
import java.util.Objects;

public final class AiJobWorkerService {
  @FunctionalInterface
  public interface JobHandler {
    void run(AiJobRequest request, AiExecutionContext context);
  }

  private final AiJobStatusStore store;
  private final ModelExecutionScheduler scheduler;
  private final JobHandler handler;
  private Duration lastBackoff = Duration.ZERO;

  public AiJobWorkerService(
      AiJobStatusStore store, ModelExecutionScheduler scheduler, JobHandler handler) {
    this.store = Objects.requireNonNull(store);
    this.scheduler = Objects.requireNonNull(scheduler);
    this.handler = Objects.requireNonNull(handler);
  }

  public void handle(AiJobRequest request) {
    if (store.claim(request.jobId(), request.context()) != AiJobStatus.CLAIMED) return;
    try {
      handler.run(request, request.context());
      store.complete(request.jobId(), request.context());
    } catch (RuntimeException failure) {
      lastBackoff = Duration.ofSeconds(1);
      store.fail(request.jobId(), "AI_JOB_RETRY_EXHAUSTED", request.context());
    }
  }

  public Duration lastBackoff() {
    return lastBackoff;
  }
}
