package com.emme.studio.api.usecase;

import com.emme.studio.application.result.AppointmentView;
import java.time.Instant;
import java.util.UUID;

/** Reschedules an appointment after validating collisions. */
public interface RescheduleAppointmentUseCase {

  AppointmentView reschedule(UUID id, Instant startsAt, Instant endsAt);
}
