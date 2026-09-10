package com.emme.assistant.application.service;

import com.emme.assistant.api.query.GetConversationQuery;
import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.usecase.GetConversationUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetConversationService implements GetConversationUseCase {
  private final ConversationRepository repository;

  @Override
  public Optional<ConversationDetails> get(GetConversationQuery query) {
    return repository.findById(query.conversationId()).map(AssistantApplicationMapper::toResult);
  }
}
