package com.emme.appointments.api.usecase;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.api.command.CreateAppointmentHoldCommand;

public interface CreateAppointmentHoldUseCase {

  AppointmentHold create(CreateAppointmentHoldCommand command);
}
