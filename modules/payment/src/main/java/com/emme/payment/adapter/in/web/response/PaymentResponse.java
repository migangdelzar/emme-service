package com.emme.payment.adapter.in.web.response;

import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.api.type.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    String providerReference,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    Instant updatedAt) {
  public static PaymentResponse from(PaymentDetails payment) {
    return new PaymentResponse(
        payment.id(),
        payment.providerReference(),
        payment.amount(),
        payment.currency(),
        payment.status(),
        payment.updatedAt());
  }
}
