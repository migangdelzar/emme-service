package com.emme.identity.application.process;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.emme.identity.api.command.ProvisionTenantIdentityCommand;
import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import com.emme.identity.application.port.out.IdentityRealmProvisioningConfigurationPort;
import com.emme.identity.application.port.out.IdentityRealmProvisioningSettings;
import com.emme.identity.application.port.out.RetryDelayPort;
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
    ProvisionTenantIdentityCommand command =
        new ProvisionTenantIdentityCommand(tenantId, "demo-salon", "Demo Salon", "owner@test");
    IdentityRealmProvisioningSettings settings = configuredSettings();

    new KeycloakRealmProvisioningProcessManager(
            administrationPort, tenantApi, configuration(settings), noOpDelay())
        .provision(command);

    verify(administrationPort).createRealm("emme-demo-salon", "Demo Salon");
    verify(administrationPort)
        .createClient("emme-demo-salon", settings.clientId(), settings.redirectUris());
    verify(administrationPort).createRealmRole("emme-demo-salon", "business_owner");
    verify(administrationPort).createRealmRole("emme-demo-salon", "nail_artist");
    verify(administrationPort).createRealmRole("emme-demo-salon", "front_desk");
    verify(administrationPort).createRealmRole("emme-demo-salon", "read_only");
    verify(administrationPort)
        .createUser(
            "emme-demo-salon",
            settings.initialAdminUsername(),
            "owner@test",
            settings.initialAdminPassword(),
            settings.initialAdminRole());
    verify(tenantApi).updateIdentityRealm(tenantId, "emme-demo-salon");
  }

  @Test
  void failsBeforeCallingTheProviderWhenProvisioningPasswordIsMissing() {
    IdentityRealmProvisioningSettings settings = configuredSettings("", 3, 2_000L);

    assertThatThrownBy(
            () ->
                new KeycloakRealmProvisioningProcessManager(
                        administrationPort, tenantApi, configuration(settings), noOpDelay())
                    .provision(
                        new ProvisionTenantIdentityCommand(
                            UUID.randomUUID(), "demo-salon", "Demo Salon", "owner@test")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Identity realm provisioning password is not configured");

    verifyNoInteractions(administrationPort, tenantApi);
  }

  @Test
  void retriesProviderFailuresWithoutBlockingTheTest() throws Exception {
    IdentityRealmProvisioningSettings settings = configuredSettings("tenant-password", 2, 2_000L);
    org.mockito.Mockito.doThrow(new IOException("provider unavailable"))
        .when(administrationPort)
        .createRealm("emme-demo-salon", "Demo Salon");

    assertThatThrownBy(
            () ->
                new KeycloakRealmProvisioningProcessManager(
                        administrationPort, tenantApi, configuration(settings), noOpDelay())
                    .provision(
                        new ProvisionTenantIdentityCommand(
                            UUID.randomUUID(), "demo-salon", "Demo Salon", "owner@test")))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("after 2 attempts");

    org.mockito.Mockito.verify(administrationPort, org.mockito.Mockito.times(2))
        .createRealm("emme-demo-salon", "Demo Salon");
  }

  private static IdentityRealmProvisioningSettings configuredSettings() {
    return configuredSettings("tenant-password", 3, 2_000L);
  }

  private static IdentityRealmProvisioningSettings configuredSettings(
      String initialAdminPassword, int maxAttempts, long retryDelayMillis) {
    return new IdentityRealmProvisioningSettings(
        "emme-salon-app",
        java.util.List.of("http://localhost:8080/*", "http://localhost:3000/*"),
        "admin",
        initialAdminPassword,
        "business_owner",
        java.util.List.of("business_owner", "nail_artist", "front_desk", "read_only"),
        maxAttempts,
        retryDelayMillis);
  }

  private static IdentityRealmProvisioningConfigurationPort configuration(
      IdentityRealmProvisioningSettings settings) {
    return () -> settings;
  }

  private static RetryDelayPort noOpDelay() {
    return duration -> {};
  }
}
