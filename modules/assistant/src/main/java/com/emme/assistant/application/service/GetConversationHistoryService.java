package com.emme.assistant.application.service;

import com.emme.assistant.api.query.GetConversationHistoryQuery;
import com.emme.assistant.api.result.ConversationEventInfo;
import com.emme.assistant.api.usecase.GetConversationHistoryUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetConversationHistoryService implements GetConversationHistoryUseCase {
  private final ConversationEventRepository repository;

  public GetConversationHistoryService(ConversationEventRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ConversationEventInfo> get(GetConversationHistoryQuery query) {
    return repository
        .findByTenantIdAndConversationId(query.tenantId(), query.conversationId())
        .stream()
        .map(AssistantApplicationMapper::toInfo)
        .toList();
  }
}
