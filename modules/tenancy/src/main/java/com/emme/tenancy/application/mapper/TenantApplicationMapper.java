package com.emme.tenancy.application.mapper;

import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.domain.model.Tenant;

public final class TenantApplicationMapper {
  private TenantApplicationMapper() {}

  public static TenantInfo toInfo(Tenant tenant) {
    return new TenantInfo(
        tenant.id(),
        tenant.slug(),
        tenant.name(),
        null,
        tenant.status().name(),
        null,
        tenant.keycloakRealm(),
        tenant.createdAt());
  }
}
