package com.emme.assistant.ai.api.result;

import java.util.UUID;

/** Persisted identifiers and validated response for one AI conversation turn. */
public record ProcessConversationResult(UUID conversationId, UUID workflowId, String response) {}
