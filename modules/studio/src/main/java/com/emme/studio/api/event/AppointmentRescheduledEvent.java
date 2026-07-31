package com.emme.studio.api.event;

import java.time.Instant;
import java.util.UUID;

public record AppointmentRescheduledEvent(
    UUID eventId,
    UUID tenantId,
    UUID appointmentId,
    Instant oldStartsAt,
    Instant oldEndsAt,
    Instant newStartsAt,
    Instant newEndsAt,
    Instant timestamp) {}
