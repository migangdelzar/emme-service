package com.emme.appointments.api.usecase;

import com.emme.appointments.api.command.RescheduleAppointmentCommand;
import com.emme.appointments.api.result.AppointmentDetails;

public interface RescheduleAuthorizedAppointmentUseCase {
  AppointmentDetails reschedule(RescheduleAppointmentCommand command);
}
