package com.emme.payment.api.command;

import java.util.Objects;
import java.util.UUID;

/** Requests a checkout link for a trusted appointment hold and workflow. */
public record CreatePaymentLinkCommand(UUID workflowId, UUID holdId, String idempotencyKey) {

  public CreatePaymentLinkCommand {
    workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    holdId = Objects.requireNonNull(holdId, "holdId must not be null");
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
