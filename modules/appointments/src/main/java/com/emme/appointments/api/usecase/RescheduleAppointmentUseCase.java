package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentDetails;
import java.time.Instant;
import java.util.UUID;

/** Reschedules an appointment after validating collisions. */
public interface RescheduleAppointmentUseCase {

  AppointmentDetails reschedule(UUID id, Instant startsAt, Instant endsAt);
}
