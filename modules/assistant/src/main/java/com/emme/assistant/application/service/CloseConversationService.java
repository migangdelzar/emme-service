package com.emme.assistant.application.service;

import com.emme.assistant.api.command.CloseConversationCommand;
import com.emme.assistant.api.result.ConversationInfo;
import com.emme.assistant.api.usecase.CloseConversationUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationRepository;
import com.emme.assistant.domain.model.Conversation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CloseConversationService implements CloseConversationUseCase {
  private final ConversationRepository repository;

  public CloseConversationService(ConversationRepository repository) {
    this.repository = repository;
  }

  @Override
  public ConversationInfo close(CloseConversationCommand command) {
    Conversation conversation =
        AssistantServiceSupport.conversation(repository, command.conversationId());
    conversation.close();
    return AssistantApplicationMapper.toInfo(repository.save(conversation));
  }
}
