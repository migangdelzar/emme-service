package com.emme.ai.contracts.job;

import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;
import java.util.UUID;

public record AiJobRequest(UUID jobId, AiJobType type, String payload, AiExecutionContext context) {
  public AiJobRequest {
    Objects.requireNonNull(jobId);
    Objects.requireNonNull(type);
    Objects.requireNonNull(context);
    if (payload == null || payload.isBlank())
      throw new IllegalArgumentException("payload must not be blank");
  }
}
