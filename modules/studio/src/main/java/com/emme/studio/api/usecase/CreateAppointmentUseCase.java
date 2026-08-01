package com.emme.studio.api.usecase;

import com.emme.studio.application.result.AppointmentView;
import java.time.Instant;
import java.util.UUID;

/** Creates an appointment after validating references and collisions. */
public interface CreateAppointmentUseCase {

  AppointmentView create(
      UUID tenantId,
      UUID customerId,
      UUID serviceId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt);
}
