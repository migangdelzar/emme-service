package com.emme.payment.api.type;

/** Public payment lifecycle vocabulary. */
public enum PaymentStatus {
  PENDING,
  AUTHORIZED,
  CAPTURED,
  DECLINED,
  REFUNDED,
  CANCELLED
}
