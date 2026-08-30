package com.emme.assistant.api.result;

import java.time.Instant;
import java.util.UUID;

public record ConversationEventDetails(
    UUID id,
    UUID tenantId,
    UUID conversationId,
    int sequenceNumber,
    String eventType,
    String payload,
    Instant occurredAt,
    String idempotencyKey,
    UUID idempotencyPrincipalId) {

  public ConversationEventDetails(
      UUID id,
      UUID tenantId,
      UUID conversationId,
      int sequenceNumber,
      String eventType,
      String payload,
      Instant occurredAt,
      String idempotencyKey) {
    this(
        id,
        tenantId,
        conversationId,
        sequenceNumber,
        eventType,
        payload,
        occurredAt,
        idempotencyKey,
        null);
  }

  public ConversationEventDetails(
      UUID id,
      UUID tenantId,
      UUID conversationId,
      int sequenceNumber,
      String eventType,
      String payload,
      Instant occurredAt) {
    this(id, tenantId, conversationId, sequenceNumber, eventType, payload, occurredAt, null, null);
  }
}
