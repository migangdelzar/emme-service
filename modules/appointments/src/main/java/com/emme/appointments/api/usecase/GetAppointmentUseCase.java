package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentDetails;
import java.util.Optional;
import java.util.UUID;

/** Retrieves an appointment by identifier. */
public interface GetAppointmentUseCase {

  Optional<AppointmentDetails> get(UUID id);
}
