package com.emme.studio.api.usecase;

import com.emme.studio.api.result.AppointmentDetails;
import java.util.UUID;

/** Cancels an appointment. */
public interface CancelAppointmentUseCase {

  AppointmentDetails cancel(UUID id);
}
