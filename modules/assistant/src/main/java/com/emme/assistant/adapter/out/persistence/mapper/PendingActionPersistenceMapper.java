package com.emme.assistant.adapter.out.persistence.mapper;

import com.emme.assistant.adapter.out.persistence.entity.PendingActionEntity;
import com.emme.assistant.domain.model.PendingAction;
import org.springframework.stereotype.Component;

@Component
public class PendingActionPersistenceMapper {
  public PendingAction toDomain(PendingActionEntity entity) {
    return new PendingAction(
        entity.getId(),
        entity.getTenantId(),
        entity.getConversationId(),
        entity.getActionType(),
        entity.getStatus(),
        entity.getDetails(),
        entity.getExpiresAt(),
        entity.getCreatedAtOverride());
  }

  public PendingActionEntity toEntity(PendingAction action) {
    PendingActionEntity entity =
        new PendingActionEntity(
            action.tenantId(),
            action.conversationId(),
            action.actionType(),
            action.details(),
            action.expiresAt());
    entity.restoreIdentity(action.id(), action.createdAt());
    entity.setStatus(action.status());
    return entity;
  }
}
