package com.emme.studio.api.result;

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
    String status) {}
