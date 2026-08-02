package com.emme.assistant.api.command;

import java.util.UUID;

public record AddConversationEventCommand(
    UUID tenantId, UUID conversationId, String eventType, String payload) {}
