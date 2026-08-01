package com.emme.identity.adapter.out.module.tenancy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.emme.tenancy.api.usecase.TenantApi;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantIdentityRealmAdapterTest {

  @Test
  void delegatesIdentityRealmUpdatesToTheTenantModuleContract() {
    TenantApi tenantApi = mock(TenantApi.class);
    TenantIdentityRealmAdapter adapter = new TenantIdentityRealmAdapter(tenantApi);
    UUID tenantId = UUID.randomUUID();

    adapter.updateRealm(tenantId, "emme-demo-salon");

    verify(tenantApi).updateIdentityRealm(tenantId, "emme-demo-salon");
  }
}
