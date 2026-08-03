package com.emme.studio.application.port.out;

import com.emme.studio.api.event.AppointmentCancelledEvent;
import com.emme.studio.api.event.AppointmentCreatedEvent;
import com.emme.studio.api.event.AppointmentRescheduledEvent;

/** Publishes appointment facts without coupling application services to Spring events. */
public interface AppointmentEventPublisher {

  void publish(AppointmentCreatedEvent event);

  void publish(AppointmentRescheduledEvent event);

  void publish(AppointmentCancelledEvent event);
}
