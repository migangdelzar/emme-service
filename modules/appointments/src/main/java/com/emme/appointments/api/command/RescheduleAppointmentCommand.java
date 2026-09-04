package com.emme.appointments.api.command;

import com.emme.appointments.api.type.AppointmentActor;
import java.time.Instant;
import java.util.UUID;

public record RescheduleAppointmentCommand(
    AppointmentActor actor,
    UUID appointmentId,
    Instant startsAt,
    Instant endsAt,
    boolean confirmed) {}
