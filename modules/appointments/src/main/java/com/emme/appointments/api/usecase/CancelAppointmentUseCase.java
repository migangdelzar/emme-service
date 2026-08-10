package com.emme.appointments.api.usecase;

import com.emme.appointments.api.result.AppointmentDetails;
import java.util.UUID;

/** Cancels an appointment. */
public interface CancelAppointmentUseCase {

  AppointmentDetails cancel(UUID id);
}
