package com.emme.assistant.application.port.out;

import com.emme.assistant.api.event.WhatsAppMessageReceived;

/** Publishes inbound WhatsApp messages to the configured application event mechanism. */
public interface WhatsAppMessageEventPublisher {

  void publish(WhatsAppMessageReceived event);
}
