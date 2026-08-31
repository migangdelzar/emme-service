package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.ai.contracts.job.AiJobType;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.domain.job.AiJobStatus;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class AiJobWorkerServiceTest {
  private final AiExecutionContext context =
      new AiExecutionContext(
          UUID.randomUUID(),
          UUID.randomUUID(),
          Set.of("USER"),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "trace",
          "idem");

  @Test
  void duplicateJobPublicationExecutesOnlyOnce() {
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    when(store.claim(any(), eq(context))).thenReturn(AiJobStatus.CLAIMED, AiJobStatus.COMPLETED);
    AiJobWorkerService worker =
        new AiJobWorkerService(
            store, mock(ModelExecutionScheduler.class), (request, ignored) -> {});
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
    worker.handle(request);
    worker.handle(request);
    verify(store, times(1)).complete(request.jobId(), context);
  }

  @Test
  void retryableFailureIsFailedAndEventuallyDeadLettered() {
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    when(store.claim(any(), eq(context))).thenReturn(AiJobStatus.CLAIMED);
    AiJobWorkerService worker =
        new AiJobWorkerService(
            store,
            executingScheduler(),
            (request, ignored) -> {
              throw new IllegalStateException("boom");
            });
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
    worker.handle(request);
    verify(store).fail(request.jobId(), "AI_JOB_FAILED", context);
    assertThat(worker.lastBackoff()).isPositive();
  }

  @Test
  void executesAJobAlreadyClaimedByReconciliationWithoutClaimingItAgain() {
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    AiJobWorkerService worker =
        new AiJobWorkerService(store, executingScheduler(), (request, ignored) -> {});
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);

    worker.handleClaimed(request);

    verify(store, never()).claim(any(), any());
    verify(store).complete(request.jobId(), context);
  }

  private static ModelExecutionScheduler executingScheduler() {
    return new ModelExecutionScheduler() {
      @Override
      public <T> T execute(
          com.emme.ai.contracts.model.ModelCapability capability,
          AiExecutionContext context,
          Duration timeout,
          Callable<T> operation) {
        try {
          return operation.call();
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      }
    };
  }
}
