package com.emme.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Framework-free payment aggregate; provider adapters must not leak into it. */
public final class Payment {
  private final UUID id;
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
}
