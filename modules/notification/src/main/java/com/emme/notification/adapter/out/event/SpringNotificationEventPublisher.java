package com.emme.notification.adapter.out.event;

import com.emme.notification.api.event.NotificationDelivered;
import com.emme.notification.application.port.out.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringNotificationEventPublisher implements NotificationEventPublisher {
  private final ApplicationEventPublisher publisher;

  @Override
  public void publish(NotificationDelivered event) {
    publisher.publishEvent(event);
  }
}
