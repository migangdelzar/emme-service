package com.emme.calendar.adapter.in.messaging;

import com.emme.appointments.api.event.AppointmentCancelled;
import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.appointments.api.event.AppointmentRescheduled;
import com.emme.calendar.api.event.CalendarSyncRequested;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class CalendarSyncListener {

  private final ApplicationEventPublisher eventPublisher;

  public CalendarSyncListener(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @ApplicationModuleListener(id = "calendar.appointment-created")
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

  @ApplicationModuleListener(id = "calendar.appointment-cancelled")
  public void onAppointmentCancelled(AppointmentCancelled event) {
    eventPublisher.publishEvent(
        new CalendarSyncRequested(
            event.tenantId(), event.appointmentId(), "DELETE", null, null, null, null, null));
  }

  @ApplicationModuleListener(id = "calendar.appointment-rescheduled")
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
