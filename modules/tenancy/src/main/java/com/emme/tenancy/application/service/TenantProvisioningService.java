package com.emme.tenancy.application.service;

import java.time.Instant;
import java.util.UUID;

/** Application capability for requesting and observing tenant database provisioning. */
public interface TenantProvisioningService {
  UUID requestProvisioning(String slug, String name, String timeZone, String locale);

  TenantStatus getStatus(UUID tenantId);

  record TenantStatus(String status, String schemaName, Instant lastMigratedAt, String error) {}
}
