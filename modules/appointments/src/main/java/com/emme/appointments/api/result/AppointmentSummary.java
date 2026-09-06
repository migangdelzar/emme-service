package com.emme.appointments.api.result;

import com.emme.appointments.api.type.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AppointmentSummary(
    UUID id,
    Instant startsAt,
    Instant endsAt,
    String customerName,
    String serviceName,
    String artistName,
    AppointmentStatus status) {}
