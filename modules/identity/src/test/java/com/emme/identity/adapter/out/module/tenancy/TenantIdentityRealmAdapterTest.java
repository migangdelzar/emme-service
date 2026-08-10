package com.emme.identity.adapter.out.module.tenancy;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.tenancy.api.command.UpdateTenantIdentityRealmCommand;
import com.emme.tenancy.api.usecase.UpdateTenantIdentityRealmUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantIdentityRealmAdapterTest {

  @Test
  void delegatesIdentityRealmUpdatesToTheTenantModuleContract() {
    UpdateTenantIdentityRealmUseCase updateRealm = mock(UpdateTenantIdentityRealmUseCase.class);
    TenantIdentityRealmAdapter adapter = new TenantIdentityRealmAdapter(updateRealm);
    UUID tenantId = UUID.randomUUID();

    adapter.updateRealm(tenantId, "emme-demo-salon");

    verify(updateRealm)
        .update(eq(new UpdateTenantIdentityRealmCommand(tenantId, "emme-demo-salon")));
  }
}
