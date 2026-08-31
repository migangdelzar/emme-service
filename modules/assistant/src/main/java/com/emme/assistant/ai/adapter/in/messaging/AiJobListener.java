package com.emme.assistant.ai.adapter.in.messaging;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.assistant.ai.application.service.AiJobWorkerService;
import java.util.concurrent.ExecutorService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public final class AiJobListener {
  private final AiJobWorkerService worker;
  private final ExecutorService executor;

  public AiJobListener(AiJobWorkerService worker, ExecutorService aiJobExecutor) {
    this.worker = worker;
    this.executor = aiJobExecutor;
  }

  @EventListener
  public void onJob(AiJobRequest request) {
    submit(() -> worker.handle(request));
  }

  /** Dispatches a job whose durable claim was already won by reconciliation. */
  public void onClaimedJob(AiJobRequest request) {
    submit(() -> worker.handleClaimed(request));
  }

  private void submit(Runnable task) {
    try {
      executor.execute(task);
    } catch (java.util.concurrent.RejectedExecutionException rejected) {
      // The durable row remains queued/retryable and is picked up by reconciliation.
    }
  }
}
