package com.emme.notification.adapter.out.event;

import com.emme.notification.api.event.NotificationDelivered;
import com.emme.notification.application.port.out.NotificationEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringNotificationEventPublisher implements NotificationEventPublisher {
  private final ApplicationEventPublisher publisher;

  public SpringNotificationEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(NotificationDelivered event) {
    publisher.publishEvent(event);
  }
}
