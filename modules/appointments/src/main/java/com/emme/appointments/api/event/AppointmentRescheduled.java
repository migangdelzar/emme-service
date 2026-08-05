package com.emme.appointments.api.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("emme.studio.appointment-rescheduled::#{#this.tenantId()}")
public record AppointmentRescheduled(
    UUID eventId,
    UUID tenantId,
    UUID appointmentId,
    Instant oldStartsAt,
    Instant oldEndsAt,
    Instant newStartsAt,
    Instant newEndsAt,
    Instant timestamp) {}
