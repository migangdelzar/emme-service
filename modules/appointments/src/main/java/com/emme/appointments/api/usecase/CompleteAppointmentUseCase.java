package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentDetails;
import java.util.UUID;

/** Completes an appointment. */
public interface CompleteAppointmentUseCase {

  AppointmentDetails complete(UUID id);
}
