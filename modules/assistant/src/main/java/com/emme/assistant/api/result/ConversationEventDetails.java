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
    Instant occurredAt) {}
