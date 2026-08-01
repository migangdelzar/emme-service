package com.emme.tenancy.application.service;

import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.domain.model.Tenant;
import java.util.UUID;

final class TenantServiceSupport {
  private TenantServiceSupport() {}

  static Tenant require(TenantRepository repository, UUID tenantId) {
    return repository
        .findById(tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
  }
}
