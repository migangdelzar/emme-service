package com.emme.assistant.ai.adapter.out.event;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.assistant.ai.application.port.out.AiJobPublisher;
import com.emme.kernel.context.AiExecutionContext;
import org.springframework.context.ApplicationEventPublisher;

public final class SpringModulithAiJobPublisher implements AiJobPublisher {
  private final ApplicationEventPublisher events;

  public SpringModulithAiJobPublisher(ApplicationEventPublisher events) {
    this.events = events;
  }

  @Override
  public void publish(AiJobRequest request, AiExecutionContext context) {
    events.publishEvent(request);
  }
}
