package com.emme.studio.application.port.out;

import com.emme.studio.api.event.AppointmentCancelled;
import com.emme.studio.api.event.AppointmentCreated;
import com.emme.studio.api.event.AppointmentRescheduled;

/** Publishes appointment facts without coupling application services to Spring events. */
public interface AppointmentEventPublisher {

  void publish(AppointmentCreated event);

  void publish(AppointmentRescheduled event);

  void publish(AppointmentCancelled event);
}
