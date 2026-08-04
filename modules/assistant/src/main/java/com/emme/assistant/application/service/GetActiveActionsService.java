package com.emme.assistant.application.service;

import com.emme.assistant.api.query.GetActiveActionsQuery;
import com.emme.assistant.api.result.PendingActionDetails;
import com.emme.assistant.api.usecase.GetActiveActionsUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.PendingActionRepository;
import com.emme.assistant.domain.model.ActionStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetActiveActionsService implements GetActiveActionsUseCase {
  private final PendingActionRepository repository;

  public GetActiveActionsService(PendingActionRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<PendingActionDetails> get(GetActiveActionsQuery query) {
    return repository
        .findByTenantIdAndConversationIdAndStatus(
            query.tenantId(), query.conversationId(), ActionStatus.PENDING)
        .stream()
        .map(AssistantApplicationMapper::toResult)
        .toList();
  }
}
