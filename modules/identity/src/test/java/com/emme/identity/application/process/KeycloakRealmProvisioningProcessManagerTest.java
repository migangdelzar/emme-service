package com.emme.identity.application.process;

import static org.mockito.Mockito.verify;

import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.usecase.TenantApi;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakRealmProvisioningProcessManagerTest {

  @Mock private IdentityProviderAdministrationPort administrationPort;
  @Mock private TenantApi tenantApi;

  @Test
  void provisionsTenantIdentityThroughTheApplicationPort() throws Exception {
    UUID tenantId = UUID.randomUUID();
    TenantCreated event = new TenantCreated(tenantId, "demo-salon", "Demo Salon", "owner@test");

    new KeycloakRealmProvisioningProcessManager(administrationPort, tenantApi).provision(event);

    verify(administrationPort).createRealm("emme-demo-salon", "Demo Salon");
    verify(administrationPort)
        .createClient(
            "emme-demo-salon",
            "emme-salon-app",
            List.of("http://localhost:8080/*", "http://localhost:3000/*"));
    verify(administrationPort).createRealmRole("emme-demo-salon", "business_owner");
    verify(administrationPort).createRealmRole("emme-demo-salon", "nail_artist");
    verify(administrationPort).createRealmRole("emme-demo-salon", "front_desk");
    verify(administrationPort).createRealmRole("emme-demo-salon", "read_only");
    verify(administrationPort)
        .createUser("emme-demo-salon", "admin", "owner@test", "admin123", "business_owner");
    verify(tenantApi).updateIdentityRealm(tenantId, "emme-demo-salon");
  }
}
