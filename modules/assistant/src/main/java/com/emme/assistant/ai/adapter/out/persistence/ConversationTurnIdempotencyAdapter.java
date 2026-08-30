package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.api.result.ProcessConversationResult;
import com.emme.assistant.ai.application.port.out.AiToolIdempotencyStore;
import com.emme.assistant.ai.application.port.out.ConversationTurnIdempotencyPort;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Reuses the durable AI idempotency ledger for conversation-turn reserve and replay. */
@Component
public final class ConversationTurnIdempotencyAdapter implements ConversationTurnIdempotencyPort {

  private static final String TOOL_KEY = "processConversation";

  private final AiToolIdempotencyStore idempotency;
  private final ObjectMapper objectMapper;

  public ConversationTurnIdempotencyAdapter(
      AiToolIdempotencyStore idempotency, ObjectMapper objectMapper) {
    this.idempotency = Objects.requireNonNull(idempotency, "idempotency must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public Optional<ProcessConversationResult> find(UUID conversationId, String idempotencyKey) {
    return idempotency.find(operationKey(conversationId, idempotencyKey)).map(this::deserialize);
  }

  @Override
  public boolean reserve(UUID conversationId, String idempotencyKey) {
    return idempotency.claim(operationKey(conversationId, idempotencyKey), TOOL_KEY);
  }

  @Override
  public void complete(
      UUID conversationId, String idempotencyKey, ProcessConversationResult result) {
    idempotency.complete(
        operationKey(conversationId, idempotencyKey),
        new AiToolResult(
            TOOL_KEY, serialize(Objects.requireNonNull(result, "result must not be null")), true));
  }

  @Override
  public void release(UUID conversationId, String idempotencyKey) {
    idempotency.release(operationKey(conversationId, idempotencyKey));
  }

  private ProcessConversationResult deserialize(AiToolResult result) {
    if (!TOOL_KEY.equals(result.toolKey())) {
      throw new IllegalStateException("Unexpected idempotency result type for conversation turn");
    }
    try {
      return objectMapper.readValue(result.content(), ProcessConversationResult.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Unable to deserialize completed conversation turn", exception);
    }
  }

  private String serialize(ProcessConversationResult result) {
    try {
      return objectMapper.writeValueAsString(result);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize completed conversation turn", exception);
    }
  }

  private static String operationKey(UUID conversationId, String idempotencyKey) {
    Objects.requireNonNull(conversationId, "conversationId must not be null");
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("idempotencyKey must not be blank");
    }
    return TOOL_KEY + ":" + conversationId + ":" + idempotencyKey;
  }
}
