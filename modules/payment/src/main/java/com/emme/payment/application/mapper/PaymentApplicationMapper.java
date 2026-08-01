package com.emme.payment.application.mapper;

import com.emme.payment.api.result.PaymentInfo;
import com.emme.payment.domain.model.Payment;

public final class PaymentApplicationMapper {
  private PaymentApplicationMapper() {}

  public static PaymentInfo toInfo(Payment payment) {
    return new PaymentInfo(
        payment.id(),
        payment.tenantId(),
        payment.providerReference(),
        payment.amount(),
        payment.currency(),
        payment.status(),
        payment.updatedAt());
  }
}
