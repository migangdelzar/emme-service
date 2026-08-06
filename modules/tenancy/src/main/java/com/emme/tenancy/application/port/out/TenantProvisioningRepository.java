package com.emme.tenancy.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Registry lifecycle capability required by the tenant provisioning process. */
public interface TenantProvisioningRepository {

  UUID requestProvisioning(UUID tenantId, String slug, String schemaName);

  TenantProvisioningStatus findStatus(UUID tenantId);

  List<TenantProvisioningRequest> findPending();

  void markActive(UUID tenantId);

  void markFailed(UUID tenantId, String error);

  String findSchemaName(UUID tenantId);

  record TenantProvisioningStatus(
      String status, String schemaName, Instant lastMigratedAt, String error) {}

  record TenantProvisioningRequest(UUID tenantId, String slug, String schemaName) {}
}
