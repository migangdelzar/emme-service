package com.emme.payment.api.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public PaymentNotFoundException(UUID paymentId) {
    super("Payment not found: " + paymentId);
  }
}
