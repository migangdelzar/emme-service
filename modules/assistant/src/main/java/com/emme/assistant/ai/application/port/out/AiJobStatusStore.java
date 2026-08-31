package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.job.AiJobStatus;
import com.emme.kernel.context.AiExecutionContext;
import java.util.UUID;

public interface AiJobStatusStore {
  void enqueue(com.emme.ai.contracts.job.AiJobRequest request);

  AiJobStatus claim(UUID jobId, AiExecutionContext context);

  void complete(UUID jobId, AiExecutionContext context);

  void fail(UUID jobId, String errorCode, AiExecutionContext context);
}
