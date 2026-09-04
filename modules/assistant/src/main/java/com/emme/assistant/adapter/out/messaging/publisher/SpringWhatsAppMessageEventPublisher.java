package com.emme.assistant.adapter.out.messaging.publisher;

import com.emme.assistant.api.event.WhatsAppMessageReceived;
import com.emme.assistant.application.port.out.WhatsAppMessageEventPublisher;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Spring Modulith adapter for inbound WhatsApp message events. */
@Component
public final class SpringWhatsAppMessageEventPublisher implements WhatsAppMessageEventPublisher {

  private final ApplicationEventPublisher events;

  public SpringWhatsAppMessageEventPublisher(ApplicationEventPublisher events) {
    this.events = Objects.requireNonNull(events, "events must not be null");
  }

  @Override
  public void publish(WhatsAppMessageReceived event) {
    events.publishEvent(Objects.requireNonNull(event, "event must not be null"));
  }
}
