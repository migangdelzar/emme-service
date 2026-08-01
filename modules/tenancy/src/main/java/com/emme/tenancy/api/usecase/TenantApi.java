package com.emme.tenancy.api.usecase;

import com.emme.tenancy.api.result.TenantInfo;
import java.util.List;
import java.util.UUID;

/**
 * Public use-case contract for tenant lookup and identity-realm coordination.
 *
 * <p>The legacy {@code TenantApi} name is retained for binary/source compatibility while the
 * contract is grouped under {@code api/usecase}.
 */
public interface TenantApi {
  TenantInfo getTenantInfo(UUID tenantId);

  List<TenantInfo> getAllTenants();

  List<TenantInfo> getActiveTenants();

  UUID getTenantIdBySlug(String slug);

  void updateIdentityRealm(UUID tenantId, String identityRealm);
}
