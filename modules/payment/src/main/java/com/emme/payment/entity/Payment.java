package com.emme.payment.entity;

import com.emme.shared.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment extends TenantOwnedEntity {

  @Column(name = "provider_reference", nullable = false, length = 150)
  private String providerReference;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency = "MXN";

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PaymentStatus status = PaymentStatus.PENDING;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Payment() {}

  public Payment(UUID tenantId, String providerReference, BigDecimal amount, String currency) {
    super(tenantId);
    this.providerReference = providerReference;
    this.amount = amount;
    this.currency = currency;
  }

  public String getProviderReference() {
    return providerReference;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void authorize() {
    if (status != PaymentStatus.PENDING)
      throw new IllegalStateException("Cannot authorize payment in " + status);
    this.status = PaymentStatus.AUTHORIZED;
    this.updatedAt = Instant.now();
  }

  public void capture() {
    if (status != PaymentStatus.AUTHORIZED)
      throw new IllegalStateException("Cannot capture payment in " + status);
    this.status = PaymentStatus.CAPTURED;
    this.updatedAt = Instant.now();
  }

  public void decline() {
    if (status != PaymentStatus.PENDING)
      throw new IllegalStateException("Cannot decline payment in " + status);
    this.status = PaymentStatus.DECLINED;
    this.updatedAt = Instant.now();
  }

  public void refund() {
    if (status != PaymentStatus.CAPTURED)
      throw new IllegalStateException("Cannot refund payment in " + status);
    this.status = PaymentStatus.REFUNDED;
    this.updatedAt = Instant.now();
  }

  /** Direct status setter for provider-driven transitions */
  public void setStatus(PaymentStatus status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }
}
