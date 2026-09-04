package com.emme.appointments.api.command;

import com.emme.appointments.api.type.AppointmentActor;
import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentCommand(
    AppointmentActor actor,
    UUID customerId,
    UUID serviceId,
    UUID artistId,
    Instant startsAt,
    Instant endsAt,
    boolean confirmed) {}
