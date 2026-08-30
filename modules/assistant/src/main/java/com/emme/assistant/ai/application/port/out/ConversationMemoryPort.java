package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.api.result.ConversationEventDetails;
import com.emme.kernel.context.AiExecutionContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Durable, tenant-scoped conversation memory required by the AI application layer. */
public interface ConversationMemoryPort {

  ConversationSnapshot load(UUID conversationId, AiExecutionContext context);

  void appendUserMessage(UUID conversationId, String message, AiExecutionContext context);

  void appendUserMessage(
      UUID conversationId, String message, String idempotencyKey, AiExecutionContext context);

  Optional<String> findUserMessage(
      UUID conversationId, String idempotencyKey, AiExecutionContext context);

  Optional<String> findAssistantResponse(
      UUID conversationId, String idempotencyKey, AiExecutionContext context);

  void appendAssistantMessage(
      UUID conversationId, String message, String idempotencyKey, AiExecutionContext context);

  record ConversationSnapshot(UUID conversationId, List<ConversationEventDetails> events) {}
}
