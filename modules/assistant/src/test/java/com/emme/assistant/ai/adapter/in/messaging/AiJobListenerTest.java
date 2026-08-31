package com.emme.assistant.ai.adapter.in.messaging;

import static org.mockito.Mockito.*;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.ai.contracts.job.AiJobType;
import com.emme.assistant.ai.application.port.out.NoopAiJobMetrics;
import com.emme.assistant.ai.application.service.AiJobWorkerService;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class AiJobListenerTest {
  @Test
  void rejectedSubmissionDoesNotRunWorkerSynchronously() {
    AiJobWorkerService worker = mock(AiJobWorkerService.class);
    ExecutorService executor = mock(ExecutorService.class);
    doThrow(new RejectedExecutionException()).when(executor).execute(any());
    AiJobListener listener = new AiJobListener(worker, executor, NoopAiJobMetrics.INSTANCE);

    listener.onJob(request());

    verify(worker, never()).handle(any());
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
