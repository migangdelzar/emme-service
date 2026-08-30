package com.emme.assistant.ai.api.command;

import java.util.UUID;

/** Trusted request data for one durable AI conversation turn. */
public record ProcessConversationCommand(
    UUID conversationId, String message, String channel, String idempotencyKey) {}
