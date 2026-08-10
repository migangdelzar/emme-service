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
    Instant occurredAt) {}
