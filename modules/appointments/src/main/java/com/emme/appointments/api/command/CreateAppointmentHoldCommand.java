package com.emme.appointments.api.command;

import java.util.Objects;
import java.util.UUID;

/** Requests a durable hold for an already selected appointment slot. */
public record CreateAppointmentHoldCommand(UUID appointmentId, String idempotencyKey) {

  public CreateAppointmentHoldCommand {
    appointmentId = Objects.requireNonNull(appointmentId, "appointmentId must not be null");
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
