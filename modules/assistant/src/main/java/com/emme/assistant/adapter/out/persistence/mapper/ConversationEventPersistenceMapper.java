package com.emme.assistant.adapter.out.persistence.mapper;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEventEntity;
import com.emme.assistant.domain.model.ConversationEvent;
import org.springframework.stereotype.Component;

@Component
public class ConversationEventPersistenceMapper {
  public ConversationEvent toDomain(ConversationEventEntity entity) {
    return new ConversationEvent(
        entity.getId(),
        entity.getTenantId(),
        entity.getConversationId(),
        entity.getSequenceNumber(),
        entity.getEventType(),
        entity.getPayload(),
        entity.getOccurredAt());
  }

  public ConversationEventEntity toEntity(ConversationEvent event) {
    ConversationEventEntity entity =
        new ConversationEventEntity(
            event.tenantId(),
            event.conversationId(),
            event.sequenceNumber(),
            event.eventType(),
            event.payload());
    entity.restoreIdentity(event.id(), event.occurredAt());
    return entity;
  }
}
