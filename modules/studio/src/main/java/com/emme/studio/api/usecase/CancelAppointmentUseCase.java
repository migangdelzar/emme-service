package com.emme.studio.api.usecase;

import com.emme.studio.application.result.AppointmentView;
import java.util.UUID;

/** Cancels an appointment. */
public interface CancelAppointmentUseCase {

  AppointmentView cancel(UUID id);
}
