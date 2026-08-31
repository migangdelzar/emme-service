package com.emme.assistant.ai.adapter.in.messaging;

import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.configuration.AiJobProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reconciles durable jobs whose event delivery was lost. Redis/live events remain optional. */
@Component
public final class AiJobReconciliationPoller {
  private final AiJobStatusStore store;
  private final AiJobListener listener;
  private final AiJobProperties properties;

  public AiJobReconciliationPoller(
      AiJobStatusStore store, AiJobListener listener, AiJobProperties properties) {
    this.store = store;
    this.listener = listener;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${app.ai.jobs.reconciliation-delay-ms:5000}")
  public void reconcile() {
    store.findAvailable(properties.pollLimit()).forEach(listener::onJob);
  }
}
