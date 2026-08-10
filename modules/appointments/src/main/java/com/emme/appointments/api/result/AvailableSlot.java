package com.emme.appointments.api.result;

import java.time.Instant;
import java.util.UUID;

/** Internal application result for an available appointment slot. */
public record AvailableSlot(UUID artistId, Instant startsAt, Instant endsAt) {}
