package com.emme.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer_membership", schema = "emme_core")
@IdClass(CustomerMembership.MembershipId.class)
public class CustomerMembership {

  @Id
  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Id
  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public CustomerMembership() {}

  public CustomerMembership(UUID customerId, UUID tenantId) {
    this.customerId = customerId;
    this.tenantId = tenantId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public void setCustomerId(UUID customerId) {
    this.customerId = customerId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public static class MembershipId implements Serializable {
    private UUID customerId;
    private UUID tenantId;

    public MembershipId() {}

    public MembershipId(UUID customerId, UUID tenantId) {
      this.customerId = customerId;
      this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MembershipId that)) return false;
      return Objects.equals(customerId, that.customerId) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(customerId, tenantId);
    }
  }
}
