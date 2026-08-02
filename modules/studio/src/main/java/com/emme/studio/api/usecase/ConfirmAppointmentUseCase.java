package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentDetails;
import java.util.UUID;

/** Confirms an appointment. */
public interface ConfirmAppointmentUseCase {

  AppointmentDetails confirm(UUID id);
}
