package com.emme.studio.adapter.out.messaging.publisher;

import com.emme.studio.api.event.AppointmentCancelledEvent;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import com.emme.studio.api.event.AppointmentRescheduledEvent;
import com.emme.studio.application.port.out.AppointmentEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes public appointment facts through Spring Modulith's event infrastructure. */
@Component
public class SpringAppointmentEventPublisher implements AppointmentEventPublisher {

  private final ApplicationEventPublisher publisher;

  public SpringAppointmentEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(AppointmentCreatedEvent event) {
    publisher.publishEvent(event);
  }

  @Override
  public void publish(AppointmentRescheduledEvent event) {
    publisher.publishEvent(event);
  }

  @Override
  public void publish(AppointmentCancelledEvent event) {
    publisher.publishEvent(event);
  }
}
