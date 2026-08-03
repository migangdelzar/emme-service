package com.emme.assistant.api.command;

import com.emme.assistant.domain.model.ActionType;
import java.time.Instant;
import java.util.UUID;

public record ProposePendingActionCommand(
    UUID tenantId, UUID conversationId, ActionType actionType, String details, Instant expiresAt) {}
