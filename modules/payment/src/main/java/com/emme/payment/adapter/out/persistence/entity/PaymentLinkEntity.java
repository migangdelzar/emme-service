package com.emme.payment.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Tenant-schema representation of a durable provider checkout link. */
@Entity
@Table(name = "payment_link")
public class PaymentLinkEntity extends TenantOwnedEntity {

  @Column(name = "workflow_id", nullable = false)
  private UUID workflowId;

  @Column(name = "provider", nullable = false, length = 80)
  private String provider;

  @Column(name = "checkout_url", nullable = false, columnDefinition = "TEXT")
  private String checkoutUrl;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "idempotency_key", nullable = false, length = 200)
  private String idempotencyKey;

  protected PaymentLinkEntity() {}

  @SuppressWarnings("this-escape")
  public PaymentLinkEntity(
      UUID tenantId,
      UUID linkId,
      UUID workflowId,
      String provider,
      String checkoutUrl,
      Instant expiresAt,
      String idempotencyKey) {
    super(tenantId);
    setId(Objects.requireNonNull(linkId, "linkId must not be null"));
    this.workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    this.provider = requireText(provider, "provider");
    this.checkoutUrl = requireText(checkoutUrl, "checkoutUrl");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
  }

  public UUID getWorkflowId() {
    return workflowId;
  }

  public String getProvider() {
    return provider;
  }

  public String getCheckoutUrl() {
    return checkoutUrl;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
