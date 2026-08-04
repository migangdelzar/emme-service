package com.emme.assistant.application.service;

import com.emme.assistant.api.command.ProposePendingActionCommand;
import com.emme.assistant.api.result.PendingActionDetails;
import com.emme.assistant.api.usecase.ProposePendingActionUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationRepository;
import com.emme.assistant.application.port.out.PendingActionRepository;
import com.emme.assistant.domain.model.ActionType;
import com.emme.assistant.domain.model.PendingAction;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProposePendingActionService implements ProposePendingActionUseCase {
  private final ConversationRepository conversations;
  private final PendingActionRepository actions;

  public ProposePendingActionService(
      ConversationRepository conversations, PendingActionRepository actions) {
    this.conversations = conversations;
    this.actions = actions;
  }

  @Override
  public PendingActionDetails propose(ProposePendingActionCommand command) {
    var conversation =
        AssistantServiceSupport.conversation(
            conversations, command.tenantId(), command.conversationId());
    return AssistantApplicationMapper.toResult(
        actions.save(
            new PendingAction(
                UUID.randomUUID(),
                command.tenantId(),
                command.conversationId(),
                ActionType.valueOf(command.actionType().name()),
                com.emme.assistant.domain.model.ActionStatus.PENDING,
                command.details(),
                command.expiresAt(),
                java.time.Instant.now())));
  }
}
