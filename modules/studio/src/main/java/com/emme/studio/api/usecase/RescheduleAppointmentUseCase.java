package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentDetails;
import java.time.Instant;
import java.util.UUID;

/** Reschedules an appointment after validating collisions. */
public interface RescheduleAppointmentUseCase {

  AppointmentDetails reschedule(UUID id, Instant startsAt, Instant endsAt);
}
