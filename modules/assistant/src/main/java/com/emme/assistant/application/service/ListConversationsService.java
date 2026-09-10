package com.emme.assistant.application.service;

import com.emme.assistant.api.query.ListConversationsQuery;
import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.usecase.ListConversationsUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListConversationsService implements ListConversationsUseCase {
  private final ConversationRepository repository;

  @Override
  public List<ConversationDetails> list(ListConversationsQuery query) {
    return repository.findAll().stream().map(AssistantApplicationMapper::toResult).toList();
  }
}
