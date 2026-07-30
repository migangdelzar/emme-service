package com.emme.tenancy.service;

import java.time.Instant;
import java.util.UUID;

public interface TenantProvisioningService {
  UUID requestProvisioning(String slug, String name, String timeZone, String locale);

  TenantStatus getStatus(UUID tenantId);

  record TenantStatus(String status, String schemaName, Instant lastMigratedAt, String error) {}
}
