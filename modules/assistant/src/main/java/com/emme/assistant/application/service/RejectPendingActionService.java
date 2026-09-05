package com.emme.assistant.application.service;

import com.emme.assistant.api.command.RejectPendingActionCommand;
import com.emme.assistant.api.result.PendingActionDetails;
import com.emme.assistant.api.usecase.RejectPendingActionUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.PendingActionRepository;
import com.emme.assistant.domain.model.ActionStatus;
import com.emme.assistant.domain.model.PendingAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RejectPendingActionService implements RejectPendingActionUseCase {
  private final PendingActionRepository repository;

  public RejectPendingActionService(PendingActionRepository repository) {
    this.repository = repository;
  }

  @Override
  public PendingActionDetails reject(RejectPendingActionCommand command) {
    PendingAction action = AssistantServiceSupport.action(repository, command.actionId());
    if (action.status() != ActionStatus.PENDING) {
      throw new IllegalStateException("Action not in PENDING state: " + command.actionId());
    }
    return AssistantApplicationMapper.toResult(
        repository.save(
            new PendingAction(
                action.id(),
                action.tenantId(),
                action.conversationId(),
                action.actionType(),
                ActionStatus.REJECTED,
                action.details(),
                action.expiresAt(),
                action.createdAt())));
  }
}
