package com.emme.assistant.application.service;

import com.emme.assistant.api.query.ListConversationsQuery;
import com.emme.assistant.api.result.ConversationInfo;
import com.emme.assistant.api.usecase.ListConversationsUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListConversationsService implements ListConversationsUseCase {
  private final ConversationRepository repository;

  public ListConversationsService(ConversationRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ConversationInfo> list(ListConversationsQuery query) {
    return repository.findByTenantId(query.tenantId()).stream()
        .map(AssistantApplicationMapper::toInfo)
        .toList();
  }
}
