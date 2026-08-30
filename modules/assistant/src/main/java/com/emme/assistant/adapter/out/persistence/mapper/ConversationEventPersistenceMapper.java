package com.emme.assistant.adapter.out.persistence.mapper;

import com.emme.assistant.adapter.out.persistence.entity.ConversationEventEntity;
import com.emme.assistant.domain.model.ConversationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ConversationEventPersistenceMapper {

  private final ObjectMapper objectMapper;

  public ConversationEventPersistenceMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ConversationEvent toDomain(ConversationEventEntity entity) {
    return new ConversationEvent(
        entity.getId(),
        entity.getTenantId(),
        entity.getConversationId(),
        entity.getSequenceNumber(),
        entity.getEventType(),
        deserializePayload(entity.getPayload()),
        entity.getOccurredAt(),
        entity.getIdempotencyKey(),
        entity.getIdempotencyPrincipalId());
  }

  public ConversationEventEntity toEntity(ConversationEvent event) {
    ConversationEventEntity entity =
        new ConversationEventEntity(
            event.tenantId(),
            event.conversationId(),
            event.sequenceNumber(),
            event.eventType(),
            serializePayload(event.payload()),
            event.idempotencyKey(),
            event.idempotencyPrincipalId());
    entity.restoreIdentity(event.id(), event.occurredAt());
    return entity;
  }

  private String deserializePayload(String payload) {
    try {
      JsonNode parsed = objectMapper.readTree(payload);
      return parsed.isTextual() ? parsed.asText() : payload;
    } catch (JsonProcessingException exception) {
      return payload;
    }
  }

  private String serializePayload(String payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize conversation event payload", exception);
    }
  }
}
