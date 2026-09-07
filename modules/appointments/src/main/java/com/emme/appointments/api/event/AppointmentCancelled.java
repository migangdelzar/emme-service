package com.emme.appointments.api.event;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCancelled(
    UUID eventId, UUID tenantId, UUID appointmentId, Instant timestamp) {}
