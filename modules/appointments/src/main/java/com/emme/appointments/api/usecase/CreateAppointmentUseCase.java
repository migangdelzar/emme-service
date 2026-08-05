package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentDetails;
import java.time.Instant;
import java.util.UUID;

/** Creates an appointment after validating references and collisions. */
public interface CreateAppointmentUseCase {

  AppointmentDetails create(
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt);
}
