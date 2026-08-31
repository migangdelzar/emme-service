package com.emme.appointments.adapter.out.persistence.adapter;

import com.emme.appointments.application.port.out.AppointmentCollisionPort;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements collision detection using the appointment persistence port. */
@Component
public class AppointmentCollisionAdapter implements AppointmentCollisionPort {

  private final AppointmentRepository appointmentRepository;

  public AppointmentCollisionAdapter(AppointmentRepository appointmentRepository) {
    this.appointmentRepository = appointmentRepository;
  }

  @Override
  public boolean hasCollision(UUID artistId, Instant startsAt, Instant endsAt) {
    return hasCollision(artistId, startsAt, endsAt, null);
  }

  @Override
  public boolean hasCollision(
      UUID artistId, Instant startsAt, Instant endsAt, UUID excludedAppointmentId) {
    return appointmentRepository
        .findByArtistIdAndOverlappingInterval(artistId, startsAt, endsAt)
        .stream()
        .anyMatch(
            appointment ->
                !appointment.getId().equals(excludedAppointmentId)
                    && (appointment.getStatus() == AppointmentStatus.CONFIRMED
                        || appointment.getStatus() == AppointmentStatus.IN_PROGRESS));
  }

  @Override
  public boolean hasCollision(
      UUID tenantId, UUID artistId, Instant startsAt, Instant endsAt, UUID excludedAppointmentId) {
    return appointmentRepository
        .findByTenantIdAndArtistIdAndOverlappingInterval(tenantId, artistId, startsAt, endsAt)
        .stream()
        .anyMatch(
            appointment ->
                !appointment.getId().equals(excludedAppointmentId)
                    && (appointment.getStatus() == AppointmentStatus.CONFIRMED
                        || appointment.getStatus() == AppointmentStatus.IN_PROGRESS));
  }
}
