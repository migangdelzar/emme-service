package com.emme.assistant.application.service;

import com.emme.assistant.api.command.AddConversationEventCommand;
import com.emme.assistant.api.result.ConversationEventInfo;
import com.emme.assistant.api.usecase.AddConversationEventUseCase;
import com.emme.assistant.application.mapper.AssistantApplicationMapper;
import com.emme.assistant.application.port.out.ConversationEventRepository;
import com.emme.assistant.application.port.out.ConversationRepository;
import com.emme.assistant.domain.model.ConversationEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddConversationEventService implements AddConversationEventUseCase {
  private final ConversationRepository conversations;
  private final ConversationEventRepository events;

  public AddConversationEventService(
      ConversationRepository conversations, ConversationEventRepository events) {
    this.conversations = conversations;
    this.events = events;
  }

  @Override
  public ConversationEventInfo add(AddConversationEventCommand command) {
    var conversation =
        AssistantServiceSupport.conversation(conversations, command.conversationId());
    int nextSequence =
        events
            .findLatestByConversationId(command.conversationId())
            .map(event -> event.sequenceNumber() + 1)
            .orElse(1);
    return AssistantApplicationMapper.toInfo(
        events.save(
            new ConversationEvent(
                java.util.UUID.randomUUID(),
                conversation.tenantId(),
                command.conversationId(),
                nextSequence,
                command.eventType(),
                command.payload(),
                java.time.Instant.now())));
  }
}
