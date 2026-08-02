package com.emme.assistant.api.query;

import java.util.UUID;

public record GetActiveActionsQuery(UUID tenantId, UUID conversationId) {}
