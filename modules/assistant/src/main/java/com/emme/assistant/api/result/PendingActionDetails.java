package com.emme.assistant.api.result;

import com.emme.assistant.api.type.ActionStatus;
import com.emme.assistant.api.type.ActionType;
import java.time.Instant;
import java.util.UUID;

public record PendingActionDetails(
    UUID id,
    UUID tenantId,
    UUID conversationId,
    ActionType actionType,
    ActionStatus status,
    String details,
    Instant expiresAt,
    Instant createdAt) {}
