package com.emme.testing;

import com.emme.identity.application.port.out.IdentityProviderAdministrationPort;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Supplies a no-op identity-provider port for full-context tests. */
@TestConfiguration
public class MockIdentityProviderAdministrationConfig {

  @Bean
  @Primary
  public IdentityProviderAdministrationPort identityProviderAdministrationPort() {
    return new NoOpIdentityProviderAdministration();
  }

  private static final class NoOpIdentityProviderAdministration
      implements IdentityProviderAdministrationPort {

    @Override
    public void createRealm(String realmName, String displayName) throws IOException {
      // No identity provider is required by H2-backed module tests.
    }

    @Override
    public void createClient(String realm, String clientId, List<String> redirectUris)
        throws IOException {
      // No identity provider is required by H2-backed module tests.
    }

    @Override
    public void createRealmRole(String realm, String roleName) throws IOException {
      // No identity provider is required by H2-backed module tests.
    }

    @Override
    public String createUser(
        String realm, String username, String email, String password, String roleName)
        throws IOException {
      return UUID.randomUUID().toString();
    }
  }
}
