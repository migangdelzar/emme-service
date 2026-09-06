package com.emme.appointments.api.usecase;

import com.emme.ai.contracts.appointment.AppointmentHold;
import java.util.Optional;
import java.util.UUID;

/** Reads a tenant-local appointment hold for workflow integrations. */
public interface GetAppointmentHoldUseCase {

  Optional<AppointmentHold> get(UUID holdId);
}
