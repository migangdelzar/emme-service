package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.api.result.ProcessConversationResult;
import java.util.Optional;
import java.util.UUID;

/** Durable reserve/replay boundary for one tenant-scoped conversation turn. */
public interface ConversationTurnIdempotencyPort {

  Optional<ProcessConversationResult> find(UUID conversationId, String idempotencyKey);

  boolean reserve(UUID conversationId, String idempotencyKey);

  void complete(UUID conversationId, String idempotencyKey, ProcessConversationResult result);

  void release(UUID conversationId, String idempotencyKey);
}
