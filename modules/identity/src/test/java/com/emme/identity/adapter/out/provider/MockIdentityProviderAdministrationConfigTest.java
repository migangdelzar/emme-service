package com.emme.identity.adapter.out.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import com.emme.testing.MockIdentityProviderAdministrationConfig;
import org.junit.jupiter.api.Test;

class MockIdentityProviderAdministrationConfigTest {

  @Test
  void exposesAProviderPortFakeWithoutDependingOnTheConcreteKeycloakAdapter() throws Exception {
    var method =
        MockIdentityProviderAdministrationConfig.class.getDeclaredMethod(
            "identityProviderAdministrationPort");

    assertThat(method.getReturnType()).isEqualTo(IdentityProviderAdministrationPort.class);
  }

  @Test
  void fakeCreatesUserReferencesForProvisioningFlows() throws Exception {
    IdentityProviderAdministrationPort provider =
        new MockIdentityProviderAdministrationConfig().identityProviderAdministrationPort();

    assertThat(provider.createUser("realm", "user", "user@example.com", "password", "owner"))
        .isNotBlank();
  }
}
