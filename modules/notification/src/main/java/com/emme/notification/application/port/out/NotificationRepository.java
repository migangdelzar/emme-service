package com.emme.notification.application.port.out;

import com.emme.notification.domain.model.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
  Optional<Notification> findByTenantIdAndId(UUID tenantId, UUID notificationId);

  List<Notification> findByTenantId(UUID tenantId);

  Notification save(Notification notification);
}
