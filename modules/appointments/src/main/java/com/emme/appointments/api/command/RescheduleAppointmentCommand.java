package com.emme.appointments.api.command;
import java.time.Instant;
import java.util.UUID;
public record RescheduleAppointmentCommand(AppointmentActor actor, UUID appointmentId, Instant startsAt, Instant endsAt, boolean confirmed) {}
