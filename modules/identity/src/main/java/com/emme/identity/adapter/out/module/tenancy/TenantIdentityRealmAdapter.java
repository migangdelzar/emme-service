package com.emme.identity.adapter.out.module.tenancy;

import com.emme.identity.application.port.out.TenantIdentityRealmPort;
import com.emme.tenancy.api.command.UpdateTenantIdentityRealmCommand;
import com.emme.tenancy.api.usecase.UpdateTenantIdentityRealmUseCase;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapts the Tenancy public contract to Identity's realm-update port. */
@Component
public class TenantIdentityRealmAdapter implements TenantIdentityRealmPort {

  private final UpdateTenantIdentityRealmUseCase updateTenantIdentityRealm;

  public TenantIdentityRealmAdapter(UpdateTenantIdentityRealmUseCase updateTenantIdentityRealm) {
    this.updateTenantIdentityRealm = updateTenantIdentityRealm;
  }

  @Override
  public void updateRealm(UUID tenantId, String identityRealm) {
    updateTenantIdentityRealm.update(new UpdateTenantIdentityRealmCommand(tenantId, identityRealm));
  }
}
