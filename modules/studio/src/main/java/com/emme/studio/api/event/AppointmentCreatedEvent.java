package com.emme.studio.api.event;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCreatedEvent(
    UUID eventId,
    UUID tenantId,
    UUID appointmentId,
    UUID customerId,
    UUID artistId,
    UUID serviceId,
    Instant startsAt,
    Instant endsAt,
    Instant timestamp) {}
