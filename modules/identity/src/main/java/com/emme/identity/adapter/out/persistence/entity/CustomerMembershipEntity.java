package com.emme.identity.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** JPA representation of the customer membership relationship. */
@Entity
@Table(name = "customer_membership", schema = "emme_core")
@IdClass(CustomerMembershipId.class)
public class CustomerMembershipEntity {

  @Id
  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Id
  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public CustomerMembershipEntity() {}

  public CustomerMembershipEntity(UUID customerId, UUID tenantId, Instant createdAt) {
    this.customerId = customerId;
    this.tenantId = tenantId;
    this.createdAt = createdAt;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
