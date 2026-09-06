package com.emme.payment.adapter.out.persistence.entity;

import com.emme.shared.persistence.TenantOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Tenant-schema mapping for provider-to-workflow callback ownership. */
@Entity
@Table(name = "ai_payment_workflow_correlation")
public class PaymentWorkflowCorrelationEntity extends TenantOwnedEntity {

  @Column(name = "workflow_id", nullable = false)
  private UUID workflowId;

  @Column(name = "provider", nullable = false, length = 80)
  private String provider;

  @Column(name = "provider_reference", nullable = false, length = 200)
  private String providerReference;

  protected PaymentWorkflowCorrelationEntity() {}

  @SuppressWarnings("this-escape")
  public PaymentWorkflowCorrelationEntity(
      UUID tenantId, UUID workflowId, String provider, String providerReference) {
    super(tenantId);
    this.workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    this.provider = requireText(provider, "provider");
    this.providerReference = requireText(providerReference, "providerReference");
  }

  public UUID getWorkflowId() {
    return workflowId;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderReference() {
    return providerReference;
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
