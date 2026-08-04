package com.emme.assistant.application.mapper;

import com.emme.assistant.api.result.ConversationEventInfo;
import com.emme.assistant.api.result.ConversationInfo;
import com.emme.assistant.api.result.PendingActionInfo;
import com.emme.assistant.api.type.ActionStatusView;
import com.emme.assistant.api.type.ActionTypeView;
import com.emme.assistant.api.type.ConversationStatusView;
import com.emme.assistant.domain.model.Conversation;
import com.emme.assistant.domain.model.ConversationEvent;
import com.emme.assistant.domain.model.PendingAction;

public final class AssistantApplicationMapper {
  private AssistantApplicationMapper() {}

  public static ConversationInfo toInfo(Conversation conversation) {
    return new ConversationInfo(
        conversation.id(),
        conversation.tenantId(),
        conversation.participantId(),
        conversation.channel(),
        ConversationStatusView.valueOf(conversation.status().name()),
        conversation.startedAt());
  }

  public static ConversationEventInfo toInfo(ConversationEvent event) {
    return new ConversationEventInfo(
        event.id(),
        event.tenantId(),
        event.conversationId(),
        event.sequenceNumber(),
        event.eventType(),
        event.payload(),
        event.occurredAt());
  }

  public static PendingActionInfo toInfo(PendingAction action) {
    return new PendingActionInfo(
        action.id(),
        action.tenantId(),
        action.conversationId(),
        ActionTypeView.valueOf(action.actionType().name()),
        ActionStatusView.valueOf(action.status().name()),
        action.details(),
        action.expiresAt(),
        action.createdAt());
  }
}
