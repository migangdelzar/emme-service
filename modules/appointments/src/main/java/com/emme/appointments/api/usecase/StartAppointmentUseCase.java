package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentDetails;
import java.util.UUID;

/** Starts an appointment. */
public interface StartAppointmentUseCase {

  AppointmentDetails start(UUID id);
}
