package com.emme.ai.contracts.payment;

import java.util.Objects;
import java.util.UUID;

/** Normalized, signature-verified provider event used to resume a payment workflow. */
public record PaymentWorkflowEvent(
    UUID tenantId,
    UUID workflowId,
    String provider,
    String eventId,
    String providerReference,
    PaymentWorkflowStatus status) {

  public PaymentWorkflowEvent {
    tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    provider = requireText(provider, "provider");
    eventId = requireText(eventId, "eventId");
    providerReference = requireText(providerReference, "providerReference");
    status = Objects.requireNonNull(status, "status must not be null");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
