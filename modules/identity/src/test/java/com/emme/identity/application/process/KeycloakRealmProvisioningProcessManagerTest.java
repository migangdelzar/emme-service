package com.emme.identity.application.process;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import com.emme.identity.application.port.out.RetryDelayPort;
import com.emme.identity.configuration.IdentityRealmProvisioningProperties;
import com.emme.tenancy.api.event.TenantCreated;
import com.emme.tenancy.api.usecase.TenantApi;
import java.io.IOException;
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
    IdentityRealmProvisioningProperties properties = configuredProperties();

    new KeycloakRealmProvisioningProcessManager(
            administrationPort, tenantApi, properties, noOpDelay())
        .provision(event);

    verify(administrationPort).createRealm("emme-demo-salon", "Demo Salon");
    verify(administrationPort)
        .createClient("emme-demo-salon", properties.getClientId(), properties.getRedirectUris());
    verify(administrationPort).createRealmRole("emme-demo-salon", "business_owner");
    verify(administrationPort).createRealmRole("emme-demo-salon", "nail_artist");
    verify(administrationPort).createRealmRole("emme-demo-salon", "front_desk");
    verify(administrationPort).createRealmRole("emme-demo-salon", "read_only");
    verify(administrationPort)
        .createUser(
            "emme-demo-salon",
            properties.getInitialAdminUsername(),
            "owner@test",
            properties.getInitialAdminPassword(),
            properties.getInitialAdminRole());
    verify(tenantApi).updateIdentityRealm(tenantId, "emme-demo-salon");
  }

  @Test
  void failsBeforeCallingTheProviderWhenProvisioningPasswordIsMissing() {
    IdentityRealmProvisioningProperties properties = configuredProperties();
    properties.setInitialAdminPassword("");

    assertThatThrownBy(
            () ->
                new KeycloakRealmProvisioningProcessManager(
                        administrationPort, tenantApi, properties, noOpDelay())
                    .provision(
                        new TenantCreated(
                            UUID.randomUUID(), "demo-salon", "Demo Salon", "owner@test")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Identity realm provisioning password is not configured");

    verifyNoInteractions(administrationPort, tenantApi);
  }

  @Test
  void retriesProviderFailuresWithoutBlockingTheTest() throws Exception {
    IdentityRealmProvisioningProperties properties = configuredProperties();
    properties.setMaxAttempts(2);
    org.mockito.Mockito.doThrow(new IOException("provider unavailable"))
        .when(administrationPort)
        .createRealm("emme-demo-salon", "Demo Salon");

    assertThatThrownBy(
            () ->
                new KeycloakRealmProvisioningProcessManager(
                        administrationPort, tenantApi, properties, noOpDelay())
                    .provision(
                        new TenantCreated(
                            UUID.randomUUID(), "demo-salon", "Demo Salon", "owner@test")))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("after 2 attempts");

    org.mockito.Mockito.verify(administrationPort, org.mockito.Mockito.times(2))
        .createRealm("emme-demo-salon", "Demo Salon");
  }

  private static IdentityRealmProvisioningProperties configuredProperties() {
    IdentityRealmProvisioningProperties properties = new IdentityRealmProvisioningProperties();
    properties.setInitialAdminPassword("tenant-password");
    return properties;
  }

  private static RetryDelayPort noOpDelay() {
    return duration -> {};
  }
}
