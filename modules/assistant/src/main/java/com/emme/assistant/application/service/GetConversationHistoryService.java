package com.emme.assistant.application.service;

import com.emme.assistant.api.query.GetConversationHistoryQuery;
import com.emme.assistant.api.result.ConversationEventDetails;
import com.emme.assistant.api.usecase.GetConversationHistoryUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetConversationHistoryService implements GetConversationHistoryUseCase {
  private final ConversationEventRepository repository;

  @Override
  public List<ConversationEventDetails> get(GetConversationHistoryQuery query) {
    return repository.findByConversationId(query.conversationId()).stream()
        .map(AssistantApplicationMapper::toResult)
        .toList();
  }
}
