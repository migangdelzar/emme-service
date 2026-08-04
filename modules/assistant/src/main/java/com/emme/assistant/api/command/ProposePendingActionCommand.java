package com.emme.assistant.api.command;

import com.emme.assistant.api.type.ActionTypeView;
import java.time.Instant;
import java.util.UUID;

public record ProposePendingActionCommand(
    UUID tenantId,
    UUID conversationId,
    ActionTypeView actionType,
    String details,
    Instant expiresAt) {}
