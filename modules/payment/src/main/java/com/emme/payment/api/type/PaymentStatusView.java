package com.emme.payment.api.type;

/** Public payment lifecycle vocabulary. */
public enum PaymentStatusView {
  PENDING,
  AUTHORIZED,
  CAPTURED,
  DECLINED,
  REFUNDED,
  CANCELLED
}
