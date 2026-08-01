package com.emme.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free relationship between a customer identity and a tenant. */
public final class CustomerMembership {

  private final UUID customerId;
  private final UUID tenantId;
  private final Instant createdAt;

  public CustomerMembership(UUID customerId, UUID tenantId) {
    this(customerId, tenantId, Instant.now());
  }

  private CustomerMembership(UUID customerId, UUID tenantId, Instant createdAt) {
    this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
    this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public static CustomerMembership rehydrate(UUID customerId, UUID tenantId, Instant createdAt) {
    return new CustomerMembership(customerId, tenantId, createdAt);
  }

  public UUID customerId() {
    return customerId;
  }

  public UUID tenantId() {
    return tenantId;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
