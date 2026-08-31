package com.emme.appointments.api.usecase;
import com.emme.appointments.api.command.CancelAppointmentCommand;
import com.emme.appointments.api.result.AppointmentDetails;
public interface CancelAuthorizedAppointmentUseCase { AppointmentDetails cancel(CancelAppointmentCommand command); }
