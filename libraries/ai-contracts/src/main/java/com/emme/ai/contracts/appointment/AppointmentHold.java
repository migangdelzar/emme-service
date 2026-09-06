package com.emme.ai.contracts.appointment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Trusted appointment hold correlation returned by the appointment application boundary. */
public record AppointmentHold(
    UUID holdId, UUID appointmentId, Instant expiresAt, String idempotencyKey) {

  public AppointmentHold {
    holdId = Objects.requireNonNull(holdId, "holdId must not be null");
    appointmentId = Objects.requireNonNull(appointmentId, "appointmentId must not be null");
    expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
