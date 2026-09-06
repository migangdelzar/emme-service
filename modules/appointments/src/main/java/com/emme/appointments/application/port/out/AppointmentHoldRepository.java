package com.emme.appointments.application.port.out;

import com.emme.ai.contracts.appointment.AppointmentHold;
import java.util.Optional;
import java.util.UUID;

/** Tenant-schema persistence boundary for durable appointment holds. */
public interface AppointmentHoldRepository {

  Optional<AppointmentHold> findByIdempotencyKey(String idempotencyKey);

  AppointmentHold save(AppointmentHold hold);

  void deleteById(UUID holdId);
}
