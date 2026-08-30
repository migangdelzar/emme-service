package com.emme.assistant.application.mapper;

import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.result.ConversationEventDetails;
import com.emme.assistant.api.result.PendingActionDetails;
import com.emme.assistant.api.type.ActionStatus;
import com.emme.assistant.api.type.ActionType;
import com.emme.assistant.api.type.ConversationStatus;
import com.emme.assistant.domain.model.Conversation;
import com.emme.assistant.domain.model.ConversationEvent;
import com.emme.assistant.domain.model.PendingAction;

public final class AssistantApplicationMapper {
  private AssistantApplicationMapper() {}

  public static ConversationDetails toResult(Conversation conversation) {
    return new ConversationDetails(
        conversation.id(),
        conversation.tenantId(),
        conversation.participantId(),
        conversation.channel(),
        ConversationStatus.valueOf(conversation.status().name()),
        conversation.startedAt());
  }

  public static ConversationEventDetails toResult(ConversationEvent event) {
    return new ConversationEventDetails(
        event.id(),
        event.tenantId(),
        event.conversationId(),
        event.sequenceNumber(),
        event.eventType(),
        event.payload(),
        event.occurredAt(),
        event.idempotencyKey(),
        event.idempotencyPrincipalId());
  }

  public static PendingActionDetails toResult(PendingAction action) {
    return new PendingActionDetails(
        action.id(),
        action.tenantId(),
        action.conversationId(),
        ActionType.valueOf(action.actionType().name()),
        ActionStatus.valueOf(action.status().name()),
        action.details(),
        action.expiresAt(),
        action.createdAt());
  }
}
