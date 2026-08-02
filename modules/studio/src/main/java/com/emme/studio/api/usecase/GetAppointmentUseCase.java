package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentDetails;
import java.util.Optional;
import java.util.UUID;

/** Retrieves an appointment by identifier. */
public interface GetAppointmentUseCase {

  Optional<AppointmentDetails> get(UUID id);
}
