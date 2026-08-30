package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.assistant.ai.application.port.out.ConversationMemoryPort;
import com.emme.assistant.api.command.AddConversationEventCommand;
import com.emme.assistant.api.query.GetConversationHistoryQuery;
import com.emme.assistant.api.query.GetConversationQuery;
import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.usecase.AddConversationEventUseCase;
import com.emme.assistant.api.usecase.GetConversationHistoryUseCase;
import com.emme.assistant.api.usecase.GetConversationUseCase;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapts existing tenant-scoped conversation application use cases to AI conversation memory. */
@Component
public class ConversationMemoryPersistenceAdapter implements ConversationMemoryPort {

  private static final String USER_MESSAGE_EVENT = "MESSAGE_RECEIVED";
  private static final String ASSISTANT_MESSAGE_EVENT = "MESSAGE_SENT";

  private final GetConversationUseCase conversations;
  private final GetConversationHistoryUseCase conversationHistory;
  private final AddConversationEventUseCase addConversationEvent;

  public ConversationMemoryPersistenceAdapter(
      GetConversationUseCase conversations,
      GetConversationHistoryUseCase conversationHistory,
      AddConversationEventUseCase addConversationEvent) {
    this.conversations = Objects.requireNonNull(conversations, "conversations must not be null");
    this.conversationHistory =
        Objects.requireNonNull(conversationHistory, "conversationHistory must not be null");
    this.addConversationEvent =
        Objects.requireNonNull(addConversationEvent, "addConversationEvent must not be null");
  }

  @Override
  public ConversationSnapshot load(UUID conversationId, AiExecutionContext context) {
    requireAccessibleConversation(conversationId, context);
    return new ConversationSnapshot(
        conversationId,
        conversationHistory.get(
            new GetConversationHistoryQuery(context.tenantId(), conversationId)));
  }

  @Override
  public void appendUserMessage(UUID conversationId, String message, AiExecutionContext context) {
    append(conversationId, message, USER_MESSAGE_EVENT, null, context);
  }

  @Override
  public Optional<String> findAssistantResponse(
      UUID conversationId, String idempotencyKey, AiExecutionContext context) {
    requireAccessibleConversation(conversationId, context);
    return conversationHistory
        .get(new GetConversationHistoryQuery(context.tenantId(), conversationId))
        .stream()
        .filter(event -> ASSISTANT_MESSAGE_EVENT.equals(event.eventType()))
        .filter(event -> idempotencyKey.equals(event.idempotencyKey()))
        .map(com.emme.assistant.api.result.ConversationEventDetails::payload)
        .findFirst();
  }

  @Override
  public void appendAssistantMessage(
      UUID conversationId, String message, String idempotencyKey, AiExecutionContext context) {
    append(conversationId, message, ASSISTANT_MESSAGE_EVENT, idempotencyKey, context);
  }

  private void append(
      UUID conversationId,
      String message,
      String eventType,
      String idempotencyKey,
      AiExecutionContext context) {
    requireAccessibleConversation(conversationId, context);
    addConversationEvent.add(
        new AddConversationEventCommand(
            context.tenantId(), conversationId, eventType, message, idempotencyKey));
  }

  private ConversationDetails requireAccessibleConversation(
      UUID conversationId, AiExecutionContext context) {
    return conversations
        .get(new GetConversationQuery(context.tenantId(), conversationId))
        .orElseThrow(
            () ->
                new SecurityException(
                    "Conversation is not accessible for the authenticated tenant"));
  }
}
