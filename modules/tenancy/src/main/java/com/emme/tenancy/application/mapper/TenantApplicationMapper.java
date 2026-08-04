package com.emme.tenancy.application.mapper;

import com.emme.tenancy.api.result.TenantDetails;
import com.emme.tenancy.domain.model.Tenant;

public final class TenantApplicationMapper {
  private TenantApplicationMapper() {}

  public static TenantDetails toResult(Tenant tenant) {
    return new TenantDetails(
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
