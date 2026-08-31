package com.emme.appointments.api.command;

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
