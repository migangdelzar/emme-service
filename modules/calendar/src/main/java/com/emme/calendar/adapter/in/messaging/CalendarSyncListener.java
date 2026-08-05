package com.emme.calendar.adapter.in.messaging;

import com.emme.calendar.api.event.CalendarSyncRequested;
import com.emme.appointments.api.event.AppointmentCancelled;
import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.appointments.api.event.AppointmentRescheduled;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class CalendarSyncListener {

  private final ApplicationEventPublisher eventPublisher;

  public CalendarSyncListener(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @ApplicationModuleListener
  public void onAppointmentCreated(AppointmentCreated event) {
    eventPublisher.publishEvent(
        new CalendarSyncRequested(
            event.tenantId(),
            event.appointmentId(),
            "CREATE",
            "Appointment",
            "",
            event.startsAt(),
            event.endsAt(),
            null));
  }

  @ApplicationModuleListener
  public void onAppointmentCancelled(AppointmentCancelled event) {
    eventPublisher.publishEvent(
        new CalendarSyncRequested(
            event.tenantId(), event.appointmentId(), "DELETE", null, null, null, null, null));
  }

  @ApplicationModuleListener
  public void onAppointmentRescheduled(AppointmentRescheduled event) {
    eventPublisher.publishEvent(
        new CalendarSyncRequested(
            event.tenantId(),
            event.appointmentId(),
            "UPDATE",
            "Appointment",
            "",
            event.newStartsAt(),
            event.newEndsAt(),
            null));
  }
}
