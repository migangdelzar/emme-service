package com.emme.assistant.adapter.in.web.mapper;

import com.emme.assistant.adapter.in.web.request.ProposeActionRequest;
import com.emme.assistant.adapter.in.web.request.StartConversationRequest;
import com.emme.assistant.api.command.ProposePendingActionCommand;
import com.emme.assistant.api.command.StartConversationCommand;
import java.util.UUID;

public final class AssistantWebMapper {
  private AssistantWebMapper() {}

  public static StartConversationCommand toCommand(
      UUID tenantId, UUID ownerId, StartConversationRequest request) {
    return new StartConversationCommand(tenantId, ownerId, request.channel());
  }

  public static ProposePendingActionCommand toCommand(
      UUID tenantId, UUID conversationId, ProposeActionRequest request) {
    return new ProposePendingActionCommand(
        tenantId, conversationId, request.actionType(), request.details(), request.expiresAt());
  }
}
