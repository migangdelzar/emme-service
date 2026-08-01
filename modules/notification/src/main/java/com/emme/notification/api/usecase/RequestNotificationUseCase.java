package com.emme.notification.api.usecase;

import com.emme.kernel.type.NotificationChannel;
import java.util.UUID;

public interface RequestNotificationUseCase {
  UUID request(UUID tenantId, NotificationChannel channel, String recipient, String message);
}
