package com.emme.notification.api.usecase;

import com.emme.notification.api.command.CancelNotificationCommand;
import com.emme.notification.api.result.NotificationInfo;

public interface CancelNotificationUseCase {
  NotificationInfo cancel(CancelNotificationCommand command);
}
