package com.emme.assistant.api.command;

import java.util.UUID;

public record AddConversationEventCommand(
    UUID tenantId,
    UUID conversationId,
    String eventType,
    String payload,
    String idempotencyKey,
    UUID idempotencyPrincipalId) {

  public AddConversationEventCommand(
      UUID tenantId, UUID conversationId, String eventType, String payload, String idempotencyKey) {
    this(tenantId, conversationId, eventType, payload, idempotencyKey, null);
  }

  public AddConversationEventCommand(
      UUID tenantId, UUID conversationId, String eventType, String payload) {
    this(tenantId, conversationId, eventType, payload, null, null);
  }
}
