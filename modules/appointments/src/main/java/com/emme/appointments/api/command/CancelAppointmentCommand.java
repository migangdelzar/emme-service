package com.emme.appointments.api.command;

import com.emme.appointments.api.type.AppointmentActor;
import java.util.UUID;

public record CancelAppointmentCommand(
    AppointmentActor actor, UUID appointmentId, boolean confirmed) {}
