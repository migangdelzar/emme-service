package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.ai.contracts.job.AiJobType;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
    AiJobWorkerService worker =
        new AiJobWorkerService(
            store, mock(ModelExecutionScheduler.class), (request, ignored) -> {});
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
    AtomicBoolean firstClaim = new AtomicBoolean(true);
    when(store.claimAndLoad(request.jobId(), context))
        .thenAnswer(
            ignored -> firstClaim.getAndSet(false) ? Optional.of(request) : Optional.empty());
    worker.handle(request);
    worker.handle(request);
    verify(store, times(1)).complete(request.jobId(), context);
  }

  @Test
  void executesOnlyTheCanonicalDurablePayloadAndContextAfterClaim() {
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    AiExecutionContext alteredContext =
        new AiExecutionContext(
            context.tenantId(),
            UUID.randomUUID(),
            Set.of("ADMIN"),
            context.conversationId(),
            context.workflowId(),
            "altered-trace",
            "altered-idempotency");
    AiJobRequest eventRequest =
        new AiJobRequest(
            UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "altered-payload", alteredContext);
    AiJobRequest canonicalRequest =
        new AiJobRequest(
            eventRequest.jobId(), AiJobType.GRAPH_PROJECTION, "canonical-payload", context);
    when(store.claimAndLoad(eventRequest.jobId(), alteredContext))
        .thenReturn(Optional.of(canonicalRequest));
    AtomicReference<AiJobRequest> executedRequest = new AtomicReference<>();
    AtomicReference<AiExecutionContext> executedContext = new AtomicReference<>();
    AiJobWorkerService worker =
        new AiJobWorkerService(
            store,
            executingScheduler(),
            (request, executionContext) -> {
              executedRequest.set(request);
              executedContext.set(executionContext);
            });

    worker.handle(eventRequest);

    assertThat(executedRequest.get()).isEqualTo(canonicalRequest);
    assertThat(executedContext.get()).isEqualTo(context);
    verify(store).claimAndLoad(eventRequest.jobId(), alteredContext);
    verify(store).complete(eventRequest.jobId(), context);
  }

  @Test
  void retryableFailureIsFailedAndEventuallyDeadLettered() {
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    AiJobWorkerService worker =
        new AiJobWorkerService(
            store,
            executingScheduler(),
            (request, ignored) -> {
              throw new IllegalStateException("boom");
            });
    AiJobRequest request =
        new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
    when(store.claimAndLoad(request.jobId(), context)).thenReturn(Optional.of(request));
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
    when(store.loadClaimed(request.jobId(), context)).thenReturn(Optional.of(request));

    worker.handleClaimed(request);

    verify(store).loadClaimed(request.jobId(), context);
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
