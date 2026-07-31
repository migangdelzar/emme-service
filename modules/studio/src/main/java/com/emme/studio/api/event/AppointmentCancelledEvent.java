package com.emme.studio.api.event;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCancelledEvent(
    UUID eventId, UUID tenantId, UUID appointmentId, Instant timestamp) {}
