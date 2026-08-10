package com.emme.appointments.application.port.out;

import com.emme.appointments.api.event.AppointmentCancelled;
import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.appointments.api.event.AppointmentRescheduled;

/** Publishes appointment facts without coupling application services to Spring events. */
public interface AppointmentEventPublisher {

  void publish(AppointmentCreated event);

  void publish(AppointmentRescheduled event);

  void publish(AppointmentCancelled event);
}
