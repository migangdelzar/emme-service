package com.emme.studio.api.usecase;

import com.emme.studio.application.result.AppointmentView;
import java.util.Optional;
import java.util.UUID;

/** Retrieves an appointment by identifier. */
public interface GetAppointmentUseCase {

  Optional<AppointmentView> get(UUID id);
}
