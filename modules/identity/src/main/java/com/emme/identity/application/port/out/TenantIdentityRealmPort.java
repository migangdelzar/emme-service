package com.emme.identity.application.port.out;

import java.util.UUID;

/** Updates the tenant record with the provisioned Identity-provider realm. */
public interface TenantIdentityRealmPort {

  void updateRealm(UUID tenantId, String identityRealm);
}
