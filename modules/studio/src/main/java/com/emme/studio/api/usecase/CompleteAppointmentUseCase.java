package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentDetails;
import java.util.UUID;

/** Completes an appointment. */
public interface CompleteAppointmentUseCase {

  AppointmentDetails complete(UUID id);
}
