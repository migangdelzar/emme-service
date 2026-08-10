package com.emme.assistant.adapter.in.web.response;

import com.emme.assistant.api.result.ConversationEventDetails;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
    UUID id,
    UUID conversationId,
    int sequenceNumber,
    String eventType,
    String payload,
    Instant occurredAt) {
  public static EventResponse from(ConversationEventDetails info) {
    return new EventResponse(
        info.id(),
        info.conversationId(),
        info.sequenceNumber(),
        info.eventType(),
        info.payload(),
        info.occurredAt());
  }
}
