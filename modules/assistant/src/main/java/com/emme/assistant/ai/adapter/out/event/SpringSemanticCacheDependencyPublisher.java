package com.emme.assistant.ai.adapter.out.event;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.ai.contracts.semantic.SemanticCacheDependencyPublisher;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

/** Sends cache dependency changes through Spring Modulith's durable event boundary. */
public final class SpringSemanticCacheDependencyPublisher
    implements SemanticCacheDependencyPublisher {

  private final ApplicationEventPublisher events;

  public SpringSemanticCacheDependencyPublisher(ApplicationEventPublisher events) {
    this.events = Objects.requireNonNull(events, "events must not be null");
  }

  @Override
  public void publish(SemanticCacheDependencyChanged event) {
    events.publishEvent(Objects.requireNonNull(event, "event must not be null"));
  }
}
