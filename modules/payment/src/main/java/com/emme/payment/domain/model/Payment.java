package com.emme.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Framework-free payment aggregate; provider adapters must not leak into it. */
public final class Payment {
  private UUID id;
  private final UUID tenantId;
  private final String providerReference;
  private final BigDecimal amount;
  private final String currency;
  private PaymentStatus status;
  private Instant updatedAt;

  public Payment(UUID tenantId, String providerReference, BigDecimal amount, String currency) {
    this.id = UUID.randomUUID();
    this.tenantId = tenantId;
    this.providerReference = providerReference;
    this.amount = amount;
    this.currency = currency;
    this.status = PaymentStatus.PENDING;
    this.updatedAt = Instant.now();
  }

  public static Payment restore(
      UUID id,
      UUID tenantId,
      String providerReference,
      BigDecimal amount,
      String currency,
      PaymentStatus status,
      Instant updatedAt) {
    Payment payment = new Payment(tenantId, providerReference, amount, currency);
    payment.id = id;
    payment.status = status;
    payment.updatedAt = updatedAt;
    return payment;
  }

  public UUID id() {
    return id;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public String providerReference() {
    return providerReference;
  }

  public BigDecimal amount() {
    return amount;
  }

  public String currency() {
    return currency;
  }

  public PaymentStatus status() {
    return status;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public void authorize() {
    transitionTo(PaymentStatus.AUTHORIZED, PaymentStatus.PENDING, "authorize");
  }

  public void capture() {
    transitionTo(PaymentStatus.CAPTURED, PaymentStatus.AUTHORIZED, "capture");
  }

  public void refund() {
    transitionTo(PaymentStatus.REFUNDED, PaymentStatus.CAPTURED, "refund");
  }

  public void decline() {
    transitionTo(PaymentStatus.DECLINED, PaymentStatus.PENDING, "decline");
  }

  public void applyProviderStatus(PaymentStatus providerStatus) {
    this.status =
        java.util.Objects.requireNonNull(providerStatus, "providerStatus must not be null");
    this.updatedAt = Instant.now();
  }

  private void transitionTo(PaymentStatus target, PaymentStatus required, String operation) {
    if (status != required) {
      throw new IllegalStateException("Cannot " + operation + " payment in " + status);
    }
    this.status = target;
    this.updatedAt = Instant.now();
  }
}
