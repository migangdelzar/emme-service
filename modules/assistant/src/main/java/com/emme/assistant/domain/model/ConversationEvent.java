package com.emme.assistant.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ConversationEvent(
    UUID id,
    UUID tenantId,
    UUID conversationId,
    int sequenceNumber,
    String eventType,
    String payload,
    Instant occurredAt,
    String idempotencyKey) {

  public ConversationEvent(
      UUID id,
      UUID tenantId,
      UUID conversationId,
      int sequenceNumber,
      String eventType,
      String payload,
      Instant occurredAt) {
    this(id, tenantId, conversationId, sequenceNumber, eventType, payload, occurredAt, null);
  }
}
