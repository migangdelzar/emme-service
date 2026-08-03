package com.emme.notification.api.usecase;

import com.emme.notification.api.command.RequestNotificationCommand;
import com.emme.notification.api.result.NotificationInfo;

public interface RequestNotificationUseCase {
  NotificationInfo request(RequestNotificationCommand command);
}
