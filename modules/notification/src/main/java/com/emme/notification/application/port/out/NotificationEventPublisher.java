package com.emme.notification.application.port.out;

import com.emme.notification.api.event.NotificationDeliveredEvent;

public interface NotificationEventPublisher {
  void publish(NotificationDeliveredEvent event);
}
