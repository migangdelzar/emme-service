package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentDetails;
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
