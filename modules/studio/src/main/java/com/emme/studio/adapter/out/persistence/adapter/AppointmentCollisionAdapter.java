package com.emme.studio.adapter.out.persistence.adapter;

import com.emme.studio.application.port.out.AppointmentCollisionPort;
import com.emme.studio.application.port.out.AppointmentRepository;
import com.emme.studio.domain.model.AppointmentStatus;
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
    return appointmentRepository
        .findByArtistIdAndStartsAtBetween(artistId, startsAt, endsAt)
        .stream()
        .anyMatch(
            appointment ->
                appointment.getStatus() == AppointmentStatus.CONFIRMED
                    || appointment.getStatus() == AppointmentStatus.IN_PROGRESS);
  }
}
