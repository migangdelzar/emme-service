package com.emme.notification.adapter.out.persistence.mapper;

import com.emme.notification.adapter.out.persistence.entity.NotificationEntity;
import com.emme.notification.domain.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceMapper {
  public Notification toDomain(NotificationEntity entity) {
    return Notification.rehydrate(
        entity.getId(),
        entity.getTenantId(),
        entity.getChannel(),
        entity.getRecipientReference(),
        entity.getBody(),
        entity.getStatus(),
        entity.getCreatedAt());
  }

  public NotificationEntity toEntity(Notification notification) {
    NotificationEntity entity =
        new NotificationEntity(
            notification.tenantId(),
            notification.channel(),
            notification.recipientReference(),
            notification.body());
    entity.restoreIdentity(notification.id(), notification.createdAt());
    entity.setStatus(notification.status());
    return entity;
  }
}
