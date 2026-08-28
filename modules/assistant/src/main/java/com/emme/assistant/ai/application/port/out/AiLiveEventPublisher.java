package com.emme.assistant.ai.application.port.out;

import java.time.Instant;

/** Publishes reconnectable, non-sensitive workflow status events for live channels. */
public interface AiLiveEventPublisher {

  void publish(LiveEvent event);

  record LiveEvent(String type, String status, String message, Instant occurredAt) {
    public LiveEvent {
      AiOperationalStatePort.requireLabel(type, "type");
      AiOperationalStatePort.requireLabel(status, "status");
      if (message == null) {
        message = "";
      }
      if (message.length() > 256) {
        throw new IllegalArgumentException("message must not exceed 256 characters");
      }
      if (occurredAt == null) {
        throw new NullPointerException("occurredAt must not be null");
      }
    }
  }
}
