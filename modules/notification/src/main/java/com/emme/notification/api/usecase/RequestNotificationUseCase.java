package com.emme.notification.api.usecase;

import com.emme.notification.api.command.RequestNotificationCommand;
import com.emme.notification.api.result.NotificationDetails;

public interface RequestNotificationUseCase {
  NotificationDetails request(RequestNotificationCommand command);
}
