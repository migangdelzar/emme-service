package com.emme.identity.adapter.out.module.tenancy;

import com.emme.identity.application.port.out.TenantIdentityRealmPort;
import com.emme.tenancy.api.usecase.TenantApi;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapts the Tenancy public contract to Identity's realm-update port. */
@Component
public class TenantIdentityRealmAdapter implements TenantIdentityRealmPort {

  private final TenantApi tenantApi;

  public TenantIdentityRealmAdapter(TenantApi tenantApi) {
    this.tenantApi = tenantApi;
  }

  @Override
  public void updateRealm(UUID tenantId, String identityRealm) {
    tenantApi.updateIdentityRealm(tenantId, identityRealm);
  }
}
