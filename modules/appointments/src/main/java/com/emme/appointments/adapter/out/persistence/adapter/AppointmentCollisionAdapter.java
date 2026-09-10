package com.emme.appointments.adapter.out.persistence.adapter;

import com.emme.appointments.application.port.out.AppointmentCollisionPort;
import com.emme.appointments.application.port.out.AppointmentRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implements collision detection using the appointment persistence port. */
@Component
@RequiredArgsConstructor
public class AppointmentCollisionAdapter implements AppointmentCollisionPort {

  private final AppointmentRepository appointmentRepository;

  @Override
  public boolean hasCollision(
      UUID tenantId, UUID artistId, Instant startsAt, Instant endsAt, UUID excludedAppointmentId) {
    return appointmentRepository.existsActiveCollision(
        tenantId, artistId, startsAt, endsAt, excludedAppointmentId);
  }
}
