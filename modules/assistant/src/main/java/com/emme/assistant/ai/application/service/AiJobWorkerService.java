package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.domain.job.AiJobStatus;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextBridge;
import com.emme.kernel.context.AiExecutionContextScope;
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
  private final int maxAttempts;
  private Duration lastBackoff = Duration.ZERO;

  public AiJobWorkerService(
      AiJobStatusStore store, ModelExecutionScheduler scheduler, JobHandler handler) {
    this(store, scheduler, handler, 3);
  }

  public AiJobWorkerService(
      AiJobStatusStore store,
      ModelExecutionScheduler scheduler,
      JobHandler handler,
      int maxAttempts) {
    this.store = Objects.requireNonNull(store);
    this.scheduler = Objects.requireNonNull(scheduler);
    this.handler = Objects.requireNonNull(handler);
    if (maxAttempts <= 0) throw new IllegalArgumentException("maxAttempts must be positive");
    this.maxAttempts = maxAttempts;
  }

  public void handle(AiJobRequest request) {
    AiExecutionContextScope.run(
        request.context(), () -> AiExecutionContextBridge.runCurrent(() -> handleBound(request)));
  }

  private void handleBound(AiJobRequest request) {
    if (store.claim(request.jobId(), request.context()) != AiJobStatus.CLAIMED) return;
    try {
      scheduler.execute(
          ModelCapability.GENERATION,
          request.context(),
          Duration.ofSeconds(30),
          () -> {
            handler.run(request, request.context());
            return null;
          });
      store.complete(request.jobId(), request.context());
    } catch (RuntimeException failure) {
      lastBackoff = Duration.ofSeconds(1L << Math.min(maxAttempts - 1, 30));
      store.fail(request.jobId(), "AI_JOB_FAILED", request.context());
    }
  }

  public Duration lastBackoff() {
    return lastBackoff;
  }
}
