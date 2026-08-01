package com.emme.assistant.domain.model;

import java.time.Instant;
import java.util.UUID;

public record PendingAction(
    UUID id,
    UUID tenantId,
    UUID conversationId,
    ActionType actionType,
    ActionStatus status,
    String details,
    Instant expiresAt,
    Instant createdAt) {}
