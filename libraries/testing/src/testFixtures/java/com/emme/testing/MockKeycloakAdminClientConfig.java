package com.emme.testing;

import com.emme.identity.infrastructure.KeycloakAdminClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces {@link KeycloakAdminClient} with a no-op double for L4 module tests.
 *
 * <p>{@code identity.KeycloakRealmProvisioner} listens for {@code TenantCreatedEvent} and calls the
 * real Keycloak admin API to provision a realm. Any L4 test whose setup creates a tenant (e.g.
 * {@code BaseSpringModuleTest.fullSetup()}) triggers this listener through the shared Spring
 * Modulith event bus, regardless of which module's test is running. No Keycloak server is available
 * in this test tier, so this config swaps in a double — mirroring the existing Mock* pattern used
 * for external providers elsewhere (MockPaymentProvider, MockModelProvider, MockEmailProvider, ...).
 */
@TestConfiguration
public class MockKeycloakAdminClientConfig {

  @Bean
  @Primary
  KeycloakAdminClient keycloakAdminClient(ObjectMapper json) {
    return new NoOpKeycloakAdminClient(json);
  }

  private static final class NoOpKeycloakAdminClient extends KeycloakAdminClient {

    NoOpKeycloakAdminClient(ObjectMapper json) {
      super("http://mock-keycloak.invalid", "master", "admin", "admin", json);
    }

    @Override
    public String getAdminToken() {
      return "mock-admin-token";
    }

    @Override
    public void createRealm(String realmName, String displayName) {
      // no-op: no real Keycloak admin API is available in this test tier
    }

    @Override
    public void createClient(String realm, String clientId, List<String> redirectUris) {
      // no-op
    }

    @Override
    public void createRealmRole(String realm, String roleName) {
      // no-op
    }

    @Override
    public String createUser(
        String realm, String username, String email, String password, String roleName) {
      return UUID.randomUUID().toString();
    }
  }
}
