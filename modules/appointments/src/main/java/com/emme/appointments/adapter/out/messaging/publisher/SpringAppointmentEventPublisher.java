package com.emme.appointments.adapter.out.messaging.publisher;

import com.emme.appointments.api.event.AppointmentCancelled;
import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.appointments.api.event.AppointmentRescheduled;
import com.emme.appointments.application.port.out.AppointmentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes public appointment facts through Spring Modulith's event infrastructure. */
@Component
@RequiredArgsConstructor
public class SpringAppointmentEventPublisher implements AppointmentEventPublisher {

  private final ApplicationEventPublisher publisher;

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
