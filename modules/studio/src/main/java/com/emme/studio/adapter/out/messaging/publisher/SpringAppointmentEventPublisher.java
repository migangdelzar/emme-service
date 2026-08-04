package com.emme.studio.adapter.out.messaging.publisher;

import com.emme.studio.api.event.AppointmentCancelled;
import com.emme.studio.api.event.AppointmentCreated;
import com.emme.studio.api.event.AppointmentRescheduled;
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
  public void publish(AppointmentCreated event) {
    publisher.publishEvent(event);
  }

  @Override
  public void publish(AppointmentRescheduled event) {
    publisher.publishEvent(event);
  }

  @Override
  public void publish(AppointmentCancelled event) {
    publisher.publishEvent(event);
  }
}
