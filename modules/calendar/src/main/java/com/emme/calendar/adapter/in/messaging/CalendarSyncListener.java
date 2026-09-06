package com.emme.calendar.adapter.in.messaging;

import com.emme.appointments.api.event.AppointmentCancelled;
import com.emme.appointments.api.event.AppointmentCreated;
import com.emme.appointments.api.event.AppointmentRescheduled;
import com.emme.calendar.api.event.CalendarSyncRequested;
import com.emme.tenancy.application.port.out.TenantRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class CalendarSyncListener {

  private final ApplicationEventPublisher eventPublisher;
  private final TenantRepository tenantRepository;

  public CalendarSyncListener(
      ApplicationEventPublisher eventPublisher, TenantRepository tenantRepository) {
    this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    this.tenantRepository =
        Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
  }

  @ApplicationModuleListener(id = "calendar.appointment-created")
  public void onAppointmentCreated(AppointmentCreated event) {
    eventPublisher.publishEvent(
        new CalendarSyncRequested(
            event.tenantId(),
            databaseId(event.tenantId()),
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
            event.tenantId(),
            databaseId(event.tenantId()),
            event.appointmentId(),
            "DELETE",
            null,
            null,
            null,
            null,
            null));
  }

  @ApplicationModuleListener(id = "calendar.appointment-rescheduled")
  public void onAppointmentRescheduled(AppointmentRescheduled event) {
    eventPublisher.publishEvent(
        new CalendarSyncRequested(
            event.tenantId(),
            databaseId(event.tenantId()),
            event.appointmentId(),
            "UPDATE",
            "Appointment",
            "",
            event.newStartsAt(),
            event.newEndsAt(),
            null));
  }

  private UUID databaseId(UUID tenantId) {
    return tenantRepository
        .findDatabaseIdByTenantId(tenantId)
        .orElseThrow(() -> new IllegalStateException("No database routing for tenant " + tenantId));
  }
}
