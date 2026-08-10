package com.emme.notification.api.usecase;

import com.emme.notification.api.command.CancelNotificationCommand;
import com.emme.notification.api.result.NotificationDetails;

public interface CancelNotificationUseCase {
  NotificationDetails cancel(CancelNotificationCommand command);
}
