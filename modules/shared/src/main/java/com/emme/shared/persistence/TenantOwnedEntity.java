package com.emme.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * Base class for entities owned by a tenant. Enables Hibernate tenant filtering and inherits
 * UUIDv7/timestamp support.
 */
@MappedSuperclass
public abstract class TenantOwnedEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  protected TenantOwnedEntity() {}

  protected TenantOwnedEntity(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }
}
