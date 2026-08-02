package com.emme.notification.application.port.out;

import com.emme.notification.api.event.NotificationDelivered;

public interface NotificationEventPublisher {
  void publish(NotificationDelivered event);
}
