package com.emme.assistant.adapter.out.persistence.mapper;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEntity;
import com.emme.assistant.domain.model.Conversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationPersistenceMapper {
  public Conversation toDomain(ConversationEntity entity) {
    return Conversation.rehydrate(
        entity.getId(),
        entity.getTenantId(),
        entity.getParticipantId(),
        entity.getChannel(),
        entity.getStatus(),
        entity.getStartedAt());
  }

  public ConversationEntity toEntity(Conversation conversation) {
    ConversationEntity entity =
        new ConversationEntity(
            conversation.tenantId(), conversation.participantId(), conversation.channel());
    if (conversation.id() != null) {
      entity.restoreIdentity(conversation.id(), conversation.startedAt());
    }
    updateEntity(conversation, entity);
    return entity;
  }

  public void updateEntity(Conversation conversation, ConversationEntity entity) {
    entity.setStatus(conversation.status());
  }
}
