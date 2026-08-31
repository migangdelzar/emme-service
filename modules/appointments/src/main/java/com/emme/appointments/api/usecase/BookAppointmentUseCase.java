package com.emme.appointments.api.usecase;
import com.emme.appointments.api.command.CreateAppointmentCommand;
import com.emme.appointments.api.result.AppointmentDetails;
public interface BookAppointmentUseCase { AppointmentDetails book(CreateAppointmentCommand command); }
