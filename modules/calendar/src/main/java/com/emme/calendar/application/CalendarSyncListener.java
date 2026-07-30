package com.emme.calendar.application;

import com.emme.calendar.event.CalendarSyncRequested;
import com.emme.studio.event.AppointmentCancelledEvent;
import com.emme.studio.event.AppointmentCreatedEvent;
import com.emme.studio.event.AppointmentRescheduledEvent;
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
  public void onAppointmentCreated(AppointmentCreatedEvent event) {
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
  public void onAppointmentCancelled(AppointmentCancelledEvent event) {
    eventPublisher.publishEvent(
        new CalendarSyncRequested(
            event.tenantId(), event.appointmentId(), "DELETE", null, null, null, null, null));
  }

  @ApplicationModuleListener
  public void onAppointmentRescheduled(AppointmentRescheduledEvent event) {
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
