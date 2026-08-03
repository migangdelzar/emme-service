package com.emme.notification.api.usecase;

import com.emme.notification.api.command.DeliverNotificationCommand;
import com.emme.notification.api.result.NotificationInfo;

public interface DeliverNotificationUseCase {
  NotificationInfo deliver(DeliverNotificationCommand command);
}
