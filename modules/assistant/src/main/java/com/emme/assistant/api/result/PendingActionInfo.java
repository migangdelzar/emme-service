package com.emme.assistant.api.result;

import com.emme.assistant.domain.model.ActionStatus;
import com.emme.assistant.domain.model.ActionType;
import java.time.Instant;
import java.util.UUID;

public record PendingActionInfo(
    UUID id,
    UUID tenantId,
    UUID conversationId,
    ActionType actionType,
    ActionStatus status,
    String details,
    Instant expiresAt,
    Instant createdAt) {}
