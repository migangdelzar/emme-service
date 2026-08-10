package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentDetails;
import java.util.UUID;

/** Confirms an appointment. */
public interface ConfirmAppointmentUseCase {

  AppointmentDetails confirm(UUID id);
}
