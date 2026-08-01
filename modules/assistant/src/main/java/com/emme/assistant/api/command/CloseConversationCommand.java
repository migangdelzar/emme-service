package com.emme.assistant.api.command;

import java.util.UUID;

public record CloseConversationCommand(UUID conversationId) {}
