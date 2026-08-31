package com.emme.assistant.ai.application.port.out;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.kernel.context.AiExecutionContext;

public interface AiJobPublisher {
  void publish(AiJobRequest request, AiExecutionContext context);
}
