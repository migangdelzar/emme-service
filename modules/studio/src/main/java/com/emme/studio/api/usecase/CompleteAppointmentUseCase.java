package com.emme.studio.api.usecase;

import com.emme.studio.application.result.AppointmentView;
import java.util.UUID;

/** Completes an appointment. */
public interface CompleteAppointmentUseCase {

  AppointmentView complete(UUID id);
}
