package com.emme.tenancy.application.port.out;

import java.util.List;
import java.util.UUID;

/** Registry lifecycle capability required by the tenant provisioning process. */
public interface TenantProvisioningRepository {

  List<TenantProvisioningRequest> findPending();

  void markActive(UUID tenantId);

  void markFailed(UUID tenantId, String error);

  record TenantProvisioningRequest(UUID tenantId, String slug, String schemaName) {}
}
