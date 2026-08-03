package com.emme.notification.application.service;

import com.emme.notification.api.exception.NotificationNotFoundException;
import com.emme.notification.application.port.out.NotificationRepository;
import com.emme.notification.domain.model.Notification;
import java.util.UUID;

final class NotificationServiceSupport {
  private NotificationServiceSupport() {}

  static Notification load(NotificationRepository repository, UUID tenantId, UUID notificationId) {
    return repository
        .findByTenantIdAndId(tenantId, notificationId)
        .orElseThrow(() -> new NotificationNotFoundException(notificationId));
  }
}
