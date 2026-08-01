package com.emme.payment.adapter.out.persistence.entity;

import com.emme.payment.domain.model.PaymentStatus;
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
public class PaymentEntity extends TenantOwnedEntity {

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

  protected PaymentEntity() {}

  public PaymentEntity(
      UUID tenantId, String providerReference, BigDecimal amount, String currency) {
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

  public void setStatus(PaymentStatus status) {
    this.status = status;
    this.updatedAt = Instant.now();
  }

  public void restoreIdentity(UUID id, Instant updatedAt) {
    restoreAuditFields(id, updatedAt, updatedAt);
  }
}
