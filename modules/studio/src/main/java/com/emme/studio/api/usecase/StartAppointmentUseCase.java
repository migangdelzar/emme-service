package com.emme.studio.api.usecase;

import com.emme.studio.application.result.AppointmentView;
import java.util.UUID;

/** Starts an appointment. */
public interface StartAppointmentUseCase {

  AppointmentView start(UUID id);
}
