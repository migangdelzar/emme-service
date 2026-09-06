package com.emme.ai.contracts.payment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Provider-neutral payment checkout correlation for a durable workflow. */
public record PaymentLink(
    UUID linkId, UUID workflowId, String provider, String checkoutUrl, Instant expiresAt) {

  public PaymentLink {
    linkId = Objects.requireNonNull(linkId, "linkId must not be null");
    workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    provider = requireText(provider, "provider");
    checkoutUrl = requireText(checkoutUrl, "checkoutUrl");
    expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
