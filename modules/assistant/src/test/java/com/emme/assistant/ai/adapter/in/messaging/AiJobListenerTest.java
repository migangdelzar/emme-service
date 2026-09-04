package com.emme.assistant.ai.adapter.in.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.ai.contracts.job.AiJobType;
import com.emme.assistant.ai.application.job.AiJobWorker;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.application.port.out.NoopAiJobMetrics;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class AiJobListenerTest {
  @Test
  void rejectedSubmissionDoesNotRunWorkerSynchronously() {
    AiJobWorker worker = mock(AiJobWorker.class);
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    ExecutorService executor = mock(ExecutorService.class);
    doThrow(new RejectedExecutionException()).when(executor).execute(any());
    AiJobListener listener = new AiJobListener(worker, executor, store, NoopAiJobMetrics.INSTANCE);

    listener.onJob(request());

    verify(worker, never()).handle(any());
  }

  @Test
  void rejectedClaimedSubmissionDefersTheDurableClaim() {
    AiJobWorker worker = mock(AiJobWorker.class);
    AiJobStatusStore store = mock(AiJobStatusStore.class);
    ExecutorService executor = mock(ExecutorService.class);
    doThrow(new RejectedExecutionException()).when(executor).execute(any());
    AiJobListener listener = new AiJobListener(worker, executor, store, NoopAiJobMetrics.INSTANCE);
    AiJobRequest request = request();

    listener.onClaimedJob(request);

    verify(store).defer(request.jobId(), request.context(), Duration.ofSeconds(1));
    verify(worker, never()).handleClaimed(any());
  }

  private static AiJobRequest request() {
    var context =
        new AiExecutionContext(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Set.of("USER"),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace",
            "idem");
    return new AiJobRequest(UUID.randomUUID(), AiJobType.GRAPH_PROJECTION, "payload", context);
  }
}
