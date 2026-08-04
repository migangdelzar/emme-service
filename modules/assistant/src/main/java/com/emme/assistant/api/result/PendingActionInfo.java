package com.emme.assistant.api.result;

import com.emme.assistant.api.type.ActionStatusView;
import com.emme.assistant.api.type.ActionTypeView;
import java.time.Instant;
import java.util.UUID;

public record PendingActionInfo(
    UUID id,
    UUID tenantId,
    UUID conversationId,
    ActionTypeView actionType,
    ActionStatusView status,
    String details,
    Instant expiresAt,
    Instant createdAt) {}
