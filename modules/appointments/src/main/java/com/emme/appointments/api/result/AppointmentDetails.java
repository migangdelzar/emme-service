package com.emme.appointments.api.result;

import com.emme.appointments.domain.model.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

/** Public appointment read model returned by Studio use cases. */
public record AppointmentDetails(
    UUID id,
    UUID customerId,
    String customerName,
    UUID serviceId,
    String serviceName,
    UUID artistId,
    String artistName,
    Instant startsAt,
    Instant endsAt,
    AppointmentStatus status) {}
