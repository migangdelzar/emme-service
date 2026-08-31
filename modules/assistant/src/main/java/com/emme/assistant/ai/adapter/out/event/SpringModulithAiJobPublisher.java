package com.emme.assistant.ai.adapter.out.event;

import com.emme.ai.contracts.job.AiJobRequest;
import com.emme.assistant.ai.application.port.out.AiJobPublisher;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.kernel.context.AiExecutionContext;
import org.springframework.context.ApplicationEventPublisher;

public final class SpringModulithAiJobPublisher implements AiJobPublisher {
  private final ApplicationEventPublisher events;
  private final AiJobStatusStore store;

  public SpringModulithAiJobPublisher(ApplicationEventPublisher events, AiJobStatusStore store) {
    this.events = events;
    this.store = store;
  }

  @Override
  public void publish(AiJobRequest request, AiExecutionContext context) {
    store.enqueue(request);
    events.publishEvent(request);
  }
}
