package com.emme.notification.api.usecase;

import com.emme.notification.api.command.DeliverNotificationCommand;
import com.emme.notification.api.result.NotificationDetails;

public interface DeliverNotificationUseCase {
  NotificationDetails deliver(DeliverNotificationCommand command);
}
