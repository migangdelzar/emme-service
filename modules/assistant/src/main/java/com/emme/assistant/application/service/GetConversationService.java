package com.emme.assistant.application.service;

import com.emme.assistant.api.query.GetConversationQuery;
import com.emme.assistant.api.result.ConversationInfo;
import com.emme.assistant.api.usecase.GetConversationUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetConversationService implements GetConversationUseCase {
  private final ConversationRepository repository;

  public GetConversationService(ConversationRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<ConversationInfo> get(GetConversationQuery query) {
    return repository
        .findByTenantIdAndId(query.tenantId(), query.conversationId())
        .map(AssistantApplicationMapper::toInfo);
  }
}
