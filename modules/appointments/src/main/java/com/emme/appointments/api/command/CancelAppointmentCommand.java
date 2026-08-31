package com.emme.appointments.api.command;

import java.util.UUID;

public record CancelAppointmentCommand(
    AppointmentActor actor, UUID appointmentId, boolean confirmed) {}
