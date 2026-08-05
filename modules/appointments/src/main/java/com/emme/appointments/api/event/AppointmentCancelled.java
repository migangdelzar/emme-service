package com.emme.appointments.api.event;

import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("emme.studio.appointment-cancelled::#{#this.tenantId()}")
public record AppointmentCancelled(
    UUID eventId, UUID tenantId, UUID appointmentId, Instant timestamp) {}
