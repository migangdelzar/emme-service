package com.emme.studio.api;

import java.time.Instant;
import java.util.UUID;

public record AppointmentInfo(
    UUID id,
    Instant startsAt,
    Instant endsAt,
    String customerName,
    String serviceName,
    String artistName,
    String status) {}
