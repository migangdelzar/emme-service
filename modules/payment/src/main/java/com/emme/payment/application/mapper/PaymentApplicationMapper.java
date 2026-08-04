package com.emme.payment.application.mapper;

import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.api.type.PaymentStatus;
import com.emme.payment.domain.model.Payment;

public final class PaymentApplicationMapper {
  private PaymentApplicationMapper() {}

  public static PaymentDetails toResult(Payment payment) {
    return new PaymentDetails(
        payment.id(),
        payment.tenantId(),
        payment.providerReference(),
        payment.amount(),
        payment.currency(),
        PaymentStatus.valueOf(payment.status().name()),
        payment.updatedAt());
  }
}
