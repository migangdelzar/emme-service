package com.emme.assistant.application.service;

import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.result.ConversationInfo;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationRepository;
import com.emme.assistant.domain.model.Conversation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StartConversationService implements StartConversationUseCase {
  private final ConversationRepository repository;

  public StartConversationService(ConversationRepository repository) {
    this.repository = repository;
  }

  @Override
  public ConversationInfo start(StartConversationCommand command) {
    return AssistantApplicationMapper.toInfo(
        repository.save(
            new Conversation(command.tenantId(), command.participantId(), command.channel())));
  }
}
