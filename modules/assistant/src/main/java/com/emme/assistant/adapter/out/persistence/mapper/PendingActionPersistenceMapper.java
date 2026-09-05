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
    if (action.id() != null) {
      entity.restoreIdentity(action.id(), action.createdAt());
    }
    updateEntity(action, entity);
    return entity;
  }

  public void updateEntity(PendingAction action, PendingActionEntity entity) {
    entity.setStatus(action.status());
    entity.setDetails(action.details());
    entity.setExpiresAt(action.expiresAt());
  }
}
