package com.emme.assistant.ai.adapter.in.messaging;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.assistant.ai.application.job.AiJobWorker;
import com.emme.assistant.ai.application.port.out.AiJobMetrics;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(AiJobWorker.class)
public final class AiJobListener {
  private final AiJobWorker worker;
  private final ExecutorService executor;
  private final AiJobStatusStore store;
  private final AiJobMetrics metrics;

  public AiJobListener(
      AiJobWorker worker,
      ExecutorService aiJobExecutor,
      AiJobStatusStore store,
      AiJobMetrics metrics) {
    this.worker = worker;
    this.executor = aiJobExecutor;
    this.store = store;
    this.metrics = metrics;
  }

  @EventListener
  public void onJob(AiJobRequest request) {
    submit(() -> worker.handle(request));
  }

  /** Dispatches a job whose durable claim was already won by reconciliation. */
  public void onClaimedJob(AiJobRequest request) {
    submitClaimed(request);
  }

  private void submit(Runnable task) {
    try {
      executor.execute(task);
    } catch (java.util.concurrent.RejectedExecutionException rejected) {
      // Ordinary event delivery has not claimed the durable row, so it remains retryable.
    } finally {
      if (executor instanceof java.util.concurrent.ThreadPoolExecutor boundedExecutor) {
        metrics.recordQueueDepth(boundedExecutor.getQueue().size());
      }
    }
  }

  private void submitClaimed(AiJobRequest request) {
    try {
      executor.execute(() -> worker.handleClaimed(request));
    } catch (java.util.concurrent.RejectedExecutionException rejected) {
      store.defer(request.jobId(), request.context(), Duration.ofSeconds(1));
    } finally {
      if (executor instanceof java.util.concurrent.ThreadPoolExecutor boundedExecutor) {
        metrics.recordQueueDepth(boundedExecutor.getQueue().size());
      }
    }
  }
}
