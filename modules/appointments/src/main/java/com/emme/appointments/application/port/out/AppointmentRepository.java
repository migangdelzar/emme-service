package com.emme.appointments.application.port.out;

import com.emme.appointments.domain.model.Appointment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by appointment use cases. */
public interface AppointmentRepository {

  Appointment save(Appointment appointment);

  Optional<Appointment> findById(UUID id);

  List<Appointment> findByTenantIdOrderByStartsAtDesc(UUID tenantId);

  List<Appointment> findByTenantIdAndStartsAtBetween(
      UUID tenantId, Instant startsAt, Instant endsAt);

  boolean existsActiveCollision(
      UUID tenantId, UUID artistId, Instant startsAt, Instant endsAt, UUID excludedAppointmentId);
}
