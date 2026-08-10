package com.emme.assistant.application.service;

import com.emme.assistant.api.command.ConfirmPendingActionCommand;
import com.emme.assistant.api.result.PendingActionDetails;
import com.emme.assistant.api.usecase.ConfirmPendingActionUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.PendingActionRepository;
import com.emme.assistant.domain.model.ActionStatus;
import com.emme.assistant.domain.model.PendingAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfirmPendingActionService implements ConfirmPendingActionUseCase {
  private final PendingActionRepository repository;

  public ConfirmPendingActionService(PendingActionRepository repository) {
    this.repository = repository;
  }

  @Override
  public PendingActionDetails confirm(ConfirmPendingActionCommand command) {
    PendingAction action =
        AssistantServiceSupport.action(repository, command.tenantId(), command.actionId());
    if (action.status() != ActionStatus.PENDING) {
      throw new IllegalStateException("Action not in PENDING state: " + command.actionId());
    }
    PendingAction confirmed =
        new PendingAction(
            action.id(),
            action.tenantId(),
            action.conversationId(),
            action.actionType(),
            ActionStatus.EXECUTED,
            action.details(),
            action.expiresAt(),
            action.createdAt());
    return AssistantApplicationMapper.toResult(repository.save(confirmed));
  }
}
