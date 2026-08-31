package com.emme.appointments.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** External capability used to check whether an artist's requested interval is occupied. */
public interface AppointmentCollisionPort {

  boolean hasCollision(UUID artistId, Instant startsAt, Instant endsAt);

  boolean hasCollision(UUID artistId, Instant startsAt, Instant endsAt, UUID excludedAppointmentId);

  default boolean hasCollision(
      UUID tenantId, UUID artistId, Instant startsAt, Instant endsAt, UUID excludedAppointmentId) {
    return hasCollision(artistId, startsAt, endsAt, excludedAppointmentId);
  }
}
