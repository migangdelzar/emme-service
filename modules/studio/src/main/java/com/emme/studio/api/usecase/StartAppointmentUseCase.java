package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentDetails;
import java.util.UUID;

/** Starts an appointment. */
public interface StartAppointmentUseCase {

  AppointmentDetails start(UUID id);
}
