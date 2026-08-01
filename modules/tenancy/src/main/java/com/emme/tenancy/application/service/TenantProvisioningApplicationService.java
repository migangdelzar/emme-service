package com.emme.tenancy.application.service;

import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** JDBC-backed implementation of the tenant provisioning application capability. */
@Service
@Transactional
class TenantProvisioningApplicationService implements TenantProvisioningService {

  private final TenantProvisioningRepository provisioningRepository;

  TenantProvisioningApplicationService(TenantProvisioningRepository provisioningRepository) {
    this.provisioningRepository = provisioningRepository;
  }

  @Override
  public UUID requestProvisioning(String slug, String name, String timeZone, String locale) {
    String schemaName = slug.replace('-', '_').replaceAll("[^a-z0-9_]", "_").toLowerCase();
    return provisioningRepository.requestProvisioning(slug, schemaName);
  }

  @Override
  public TenantStatus getStatus(UUID tenantId) {
    TenantProvisioningRepository.TenantProvisioningStatus status =
        provisioningRepository.findStatus(tenantId);
    return new TenantStatus(
        status.status(), status.schemaName(), status.lastMigratedAt(), status.error());
  }
}
