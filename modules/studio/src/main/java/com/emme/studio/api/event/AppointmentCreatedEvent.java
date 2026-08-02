package com.emme.studio.api.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("emme.studio.appointment-created::#{#this.tenantId()}")
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
