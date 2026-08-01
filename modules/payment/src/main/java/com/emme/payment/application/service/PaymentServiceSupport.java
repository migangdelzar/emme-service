package com.emme.payment.application.service;

import com.emme.payment.api.exception.PaymentNotFoundException;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.domain.model.Payment;
import com.emme.payment.domain.model.PaymentStatus;
import java.util.UUID;

final class PaymentServiceSupport {
  private PaymentServiceSupport() {}

  static Payment load(PaymentRepository repository, UUID tenantId, UUID paymentId) {
    return repository
        .findByTenantIdAndId(tenantId, paymentId)
        .orElseThrow(() -> new PaymentNotFoundException(paymentId));
  }

  static PaymentStatus status(String providerStatus) {
    try {
      return PaymentStatus.valueOf(providerStatus.toUpperCase());
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(
          "Unsupported payment provider status: " + providerStatus, exception);
    }
  }
}
