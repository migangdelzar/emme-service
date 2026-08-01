package com.emme.tenancy.api;

import java.util.List;
import java.util.UUID;

/**
 * Public API for tenant operations. Business modules depend on this interface, not on internal
 * entity classes.
 */
public interface TenantApi {
  TenantInfo getTenantInfo(UUID tenantId);

  List<TenantInfo> getAllTenants();

  List<TenantInfo> getActiveTenants();

  UUID getTenantIdBySlug(String slug);

  void updateIdentityRealm(UUID tenantId, String identityRealm);
}
