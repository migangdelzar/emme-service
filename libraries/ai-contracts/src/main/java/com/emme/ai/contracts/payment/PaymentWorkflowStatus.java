package com.emme.ai.contracts.payment;

/** Stable payment lifecycle vocabulary used by durable workflow events. */
public enum PaymentWorkflowStatus {
  PENDING,
  AUTHORIZED,
  CAPTURED,
  DECLINED,
  REFUNDED,
  CANCELLED
}
