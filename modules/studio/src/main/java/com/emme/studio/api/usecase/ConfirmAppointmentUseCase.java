package com.emme.studio.api.usecase;

import com.emme.studio.application.result.AppointmentView;
import java.util.UUID;

/** Confirms an appointment. */
public interface ConfirmAppointmentUseCase {

  AppointmentView confirm(UUID id);
}
