package com.emme.assistant.application.service;

import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.result.ConversationDetails;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationRepository;
import com.emme.assistant.domain.model.Conversation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StartConversationService implements StartConversationUseCase {
  private final ConversationRepository repository;

  @Override
  public ConversationDetails start(StartConversationCommand command) {
    return AssistantApplicationMapper.toResult(
        repository.save(
            new Conversation(command.tenantId(), command.participantId(), command.channel())));
  }
}
