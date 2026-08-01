package com.emme.identity.adapter.out.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite database identity for a customer-to-tenant membership. */
public final class CustomerMembershipId implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID customerId;
  private UUID tenantId;

  public CustomerMembershipId() {}

  public CustomerMembershipId(UUID customerId, UUID tenantId) {
    this.customerId = customerId;
    this.tenantId = tenantId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof CustomerMembershipId that)) return false;
    return Objects.equals(customerId, that.customerId) && Objects.equals(tenantId, that.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(customerId, tenantId);
  }
}
